(() => {
    const {request, notify, money} = window.shoptechCommerce;
    const summary = document.getElementById('checkout-summary'), form = document.getElementById('checkout-form');
    const submit = document.getElementById('place-order');
    const shippingAddressInput = document.getElementById('shippingAddress');
    const btnGpsLocate = document.getElementById('btn-gps-locate');
    const btnOpenMap = document.getElementById('btn-open-map');
    const gpsStatus = document.getElementById('gps-status-indicator');

    // Modal elements
    const mapModal = document.getElementById('map-picker-modal');
    const btnModalClose = document.getElementById('btn-modal-close');
    const btnModalCancel = document.getElementById('btn-modal-cancel');
    const btnModalConfirm = document.getElementById('btn-modal-confirm');
    const btnModalGps = document.getElementById('btn-modal-gps');
    const mapSearchInput = document.getElementById('map-search-input');
    const mapContainer = document.getElementById('google-map-element');
    const mapCurrentAddress = document.getElementById('map-current-address');
    const mapCurrentCoords = document.getElementById('map-current-coords');

    // API Key from meta tag
    const apiKeyMeta = document.querySelector('meta[name="google-maps-api-key"]');
    const googleMapsApiKey = apiKeyMeta && apiKeyMeta.content ? apiKeyMeta.content.trim() : '';

    // Keep the same key after network failure/reload so retry cannot place a second order.
    const storageKey = 'shoptech-checkout-key:' + document.querySelector('meta[name="commerce-user"]').content;
    let key = sessionStorage.getItem(storageKey);
    if (!key) { key = crypto.randomUUID(); sessionStorage.setItem(storageKey,key); }
    request('/api/cart').then(cart => {
        summary.replaceChildren();
        if (cart.items.length) {
            const heading = document.createElement('div');
            heading.className = 'checkout-summary-head';
            const productLabel = document.createElement('span'); productLabel.textContent = 'Sản phẩm';
            const priceLabel = document.createElement('span'); priceLabel.textContent = 'Thành tiền';
            heading.append(productLabel, priceLabel); summary.append(heading);
        }
        cart.items.forEach(item => {
            const row = document.createElement('div'); row.className = 'checkout-summary-item';
            const description = document.createElement('div'); description.className = 'checkout-summary-name';
            const name = document.createElement('p'); name.textContent = item.name;
            const quantity = document.createElement('small'); quantity.textContent = 'Số lượng: ' + item.quantity;
            description.append(name, quantity);
            const price = document.createElement('span'); price.className = 'checkout-summary-price'; price.textContent = money(item.subtotal);
            row.append(description, price); summary.append(row);
        });
        const total = document.createElement('div'); total.className = 'checkout-summary-total';
        const totalLabel = document.createElement('span'); totalLabel.textContent = 'Tổng thanh toán';
        const totalAmount = document.createElement('strong'); totalAmount.textContent = money(cart.total);
        total.append(totalLabel, totalAmount); summary.append(total);
        if (!cart.items.length) notify('Giỏ hàng trống. Vui lòng thêm sản phẩm.',true);
        submit.disabled = !cart.items.length;
    }).catch(error => { summary.textContent = 'Không thể tải giỏ hàng.'; notify(error.message,true); });
    let pending = false;
    form.addEventListener('submit', async event => {
        event.preventDefault(); if (pending || !form.reportValidity()) return;
        pending = true; submit.disabled = true;
        const input = Object.fromEntries(new FormData(form)); input.requestKey = key;
        try {
            const order = await request('/api/orders','POST',input);
            sessionStorage.removeItem(storageKey); window.location.assign('/orders/' + order.id);
        } catch (error) { notify(error.message,true); pending = false; submit.disabled = false; }
    });

    // =========================================================================
    // GOOGLE MAPS & GPS LOCATION ENGINE (WITH SMART RESILIENT FALLBACK)
    // =========================================================================
    let googleMapsLoaded = false;
    let googleMapsAuthFailed = false;
    let googleMapsLoadingPromise = null;
    let activeMap = null;
    let activeMarker = null;
    let activeLeafletMap = null;
    let activeLeafletMarker = null;
    let selectedLocation = { lat: 10.776889, lng: 106.700806, address: '' };
    let statusTimer = null;
    let fallbackNoticeEl = document.getElementById('map-fallback-notice');

    // Official Google Maps Authentication Failure Hook
    // Triggered when API key is invalid, missing Billing, or APIs are not enabled
    window.gm_authFailure = function() {
        console.warn('Google Maps gm_authFailure: Key requires Billing or API is not enabled. Automatically activating Leaflet OpenStreetMap fallback...');
        googleMapsAuthFailed = true;
        activateLeafletFallback(selectedLocation);
    };

    function showStatus(message, type = 'loading') {
        if (!gpsStatus) return;
        if (statusTimer) clearTimeout(statusTimer);
        gpsStatus.hidden = false;
        gpsStatus.className = 'gps-status-box status-' + type;
        const iconClass = type === 'loading' ? 'fa-spinner fa-spin' : (type === 'success' ? 'fa-check-circle' : 'fa-exclamation-triangle');
        gpsStatus.innerHTML = `<i class="fa ${iconClass}"></i> <span>${message}</span>`;
        if (type !== 'loading') {
            statusTimer = setTimeout(() => {
                gpsStatus.hidden = true;
            }, 6000);
        }
    }

    function highlightAddressInput() {
        if (!shippingAddressInput) return;
        shippingAddressInput.classList.remove('address-highlight');
        void shippingAddressInput.offsetWidth; // trigger reflow
        shippingAddressInput.classList.add('address-highlight');
    }

    // Dynamic Google Maps Loader
    function loadGoogleMapsScript() {
        if (googleMapsAuthFailed) {
            return Promise.reject(new Error('Google Maps authentication previously failed'));
        }
        if (window.google && window.google.maps) {
            googleMapsLoaded = true;
            return Promise.resolve(window.google.maps);
        }
        if (!googleMapsApiKey) {
            return Promise.reject(new Error('Chưa cấu hình GOOGLE_MAPS_API_KEY'));
        }
        if (googleMapsLoadingPromise) return googleMapsLoadingPromise;

        googleMapsLoadingPromise = new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = `https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(googleMapsApiKey)}&libraries=places&language=vi&region=VN`;
            script.async = true;
            script.defer = true;
            script.onload = () => {
                googleMapsLoaded = true;
                resolve(window.google.maps);
            };
            script.onerror = () => {
                googleMapsAuthFailed = true;
                reject(new Error('Không thể tải Google Maps API từ Google'));
            };
            document.head.appendChild(script);
        });
        return googleMapsLoadingPromise;
    }

    // Reverse Geocoding with Fallback
    async function reverseGeocodeCoords(lat, lng) {
        // 1. Try Google Geocoder if available and auth hasn't failed
        if (googleMapsApiKey && !googleMapsAuthFailed) {
            try {
                const maps = await loadGoogleMapsScript();
                const geocoder = new maps.Geocoder();
                const response = await new Promise((res, rej) => {
                    geocoder.geocode({ location: { lat, lng } }, (results, status) => {
                        if (status === 'OK' && results && results[0]) {
                            res(results[0].formatted_address);
                        } else {
                            if (status === 'REQUEST_DENIED' || status === 'OVER_QUERY_LIMIT') {
                                googleMapsAuthFailed = true;
                                activateLeafletFallback({ lat, lng });
                            }
                            rej(new Error('Google Geocoder status: ' + status));
                        }
                    });
                });
                if (response) return response;
            } catch (err) {
                console.warn('Google Maps Geocoder unavailable, using backup geocoder:', err.message);
            }
        }

        // 2. Seamless fallback to OpenStreetMap Nominatim reverse geocoding
        try {
            const url = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1&accept-language=vi`;
            const resp = await fetch(url, {
                headers: { 'Accept': 'application/json' }
            });
            if (resp.ok) {
                const data = await resp.json();
                if (data) {
                    if (data.address) {
                        const a = data.address;
                        const parts = [
                            a.house_number || a.building,
                            a.road || a.street || a.neighbourhood,
                            a.suburb || a.quarter || a.ward,
                            a.city_district || a.district || a.county,
                            a.city || a.town || a.province
                        ].filter(Boolean);
                        if (parts.length >= 2) {
                            return parts.join(', ');
                        }
                    }
                    if (data.display_name) {
                        return data.display_name;
                    }
                }
            }
        } catch (err) {
            console.warn('Backup geocoder failed:', err);
        }

        return `Vị trí tọa độ: ${lat.toFixed(6)}, ${lng.toFixed(6)}`;
    }

    // Handle Direct GPS Geolocation Button Click
    async function handleGpsLocate(buttonElement) {
        if (!navigator.geolocation) {
            showStatus('Trình duyệt của bạn không hỗ trợ định vị GPS.', 'error');
            notify('Trình duyệt không hỗ trợ Geolocation GPS.', true);
            return;
        }

        if (buttonElement) buttonElement.disabled = true;
        showStatus('Đang dò tìm tọa độ GPS của bạn...', 'loading');

        navigator.geolocation.getCurrentPosition(
            async (position) => {
                const lat = position.coords.latitude;
                const lng = position.coords.longitude;
                selectedLocation.lat = lat;
                selectedLocation.lng = lng;

                showStatus('Đang dò tìm địa chỉ chi tiết từ tọa độ GPS...', 'loading');

                try {
                    const address = await reverseGeocodeCoords(lat, lng);
                    selectedLocation.address = address;
                    if (shippingAddressInput) {
                        shippingAddressInput.value = address;
                        highlightAddressInput();
                    }
                    showStatus('✓ Đã định vị thành công: ' + address, 'success');
                    notify('Đã lấy vị trí GPS và điền địa chỉ giao hàng thành công!');
                } catch (error) {
                    showStatus('Không thể phân giải địa chỉ từ tọa độ GPS.', 'error');
                    notify('Lỗi dò tìm địa chỉ từ GPS: ' + error.message, true);
                } finally {
                    if (buttonElement) buttonElement.disabled = false;
                }
            },
            (error) => {
                if (buttonElement) buttonElement.disabled = false;
                let message = 'Không thể lấy vị trí GPS hiện tại.';
                switch (error.code) {
                    case error.PERMISSION_DENIED:
                        message = 'Bạn đã từ chối cấp quyền truy cập vị trí GPS.';
                        break;
                    case error.POSITION_UNAVAILABLE:
                        message = 'Thông tin vị trí GPS hiện không khả dụng.';
                        break;
                    case error.TIMEOUT:
                        message = 'Yêu cầu định vị GPS bị quá thời gian chờ (timeout).';
                        break;
                }
                showStatus(message, 'error');
                notify(message, true);
            },
            {
                enableHighAccuracy: true,
                timeout: 15000,
                maximumAge: 0
            }
        );
    }

    if (btnGpsLocate) {
        btnGpsLocate.addEventListener('click', () => handleGpsLocate(btnGpsLocate));
    }

    // =========================================================================
    // INTERACTIVE MAP MODAL (GOOGLE MAPS + LEAFLET FALLBACK)
    // =========================================================================
    function openMapModal() {
        if (!mapModal) return;
        mapModal.hidden = false;
        document.body.style.overflow = 'hidden';

        setTimeout(() => {
            initMapPicker();
        }, 100);
    }

    function closeMapModal() {
        if (!mapModal) return;
        mapModal.hidden = true;
        document.body.style.overflow = '';
    }

    function updateModalAddressDisplay(address, lat, lng) {
        selectedLocation.lat = lat;
        selectedLocation.lng = lng;
        selectedLocation.address = address;

        if (mapCurrentAddress) {
            mapCurrentAddress.textContent = address || 'Đang cập nhật địa chỉ...';
        }
        if (mapCurrentCoords) {
            mapCurrentCoords.textContent = `Tọa độ: ${lat.toFixed(6)}, ${lng.toFixed(6)}`;
        }
        if (btnModalConfirm) {
            btnModalConfirm.disabled = !address;
        }
    }

    async function onLocationMoved(lat, lng) {
        if (mapCurrentAddress) mapCurrentAddress.innerHTML = '<i class="fa fa-spinner fa-spin"></i> Đang dò tìm địa chỉ...';
        if (btnModalConfirm) btnModalConfirm.disabled = true;

        const address = await reverseGeocodeCoords(lat, lng);
        updateModalAddressDisplay(address, lat, lng);
    }

    // Initialize Map inside Modal
    async function initMapPicker() {
        const centerPos = { lat: selectedLocation.lat, lng: selectedLocation.lng };

        // If Google Maps previously failed authentication, switch directly to Leaflet
        if (googleMapsAuthFailed || !googleMapsApiKey) {
            activateLeafletFallback(centerPos);
            return;
        }

        // Try Google Maps first
        let googleLoadedSuccessfully = false;
        try {
            const maps = await loadGoogleMapsScript();
            mapContainer.replaceChildren();

            activeMap = new maps.Map(mapContainer, {
                center: centerPos,
                zoom: 16,
                mapTypeControl: true,
                streetViewControl: false,
                fullscreenControl: false,
                zoomControl: true
            });

            activeMarker = new maps.Marker({
                position: centerPos,
                map: activeMap,
                draggable: true,
                animation: maps.Animation.DROP,
                title: 'Kéo thả để chọn vị trí giao hàng'
            });

            activeMarker.addListener('dragend', () => {
                const pos = activeMarker.getPosition();
                onLocationMoved(pos.lat(), pos.lng());
            });

            activeMap.addListener('click', (e) => {
                activeMarker.setPosition(e.latLng);
                onLocationMoved(e.latLng.lat(), e.latLng.lng());
            });

            if (maps.places && mapSearchInput) {
                const autocomplete = new maps.places.Autocomplete(mapSearchInput, {
                    componentRestrictions: { country: 'vn' },
                    fields: ['geometry', 'formatted_address', 'name']
                });

                autocomplete.addListener('place_changed', () => {
                    const place = autocomplete.getPlace();
                    if (!place.geometry || !place.geometry.location) return;
                    activeMap.setCenter(place.geometry.location);
                    activeMap.setZoom(17);
                    activeMarker.setPosition(place.geometry.location);
                    updateModalAddressDisplay(place.formatted_address || place.name, place.geometry.location.lat(), place.geometry.location.lng());
                });
            }

            onLocationMoved(centerPos.lat, centerPos.lng);
            googleLoadedSuccessfully = true;

            // Safeguard timeout: if container remains blank or auth failed silently after 2.5s
            setTimeout(() => {
                if (googleMapsAuthFailed || (mapSearchInput && mapSearchInput.placeholder && mapSearchInput.placeholder.includes('xảy ra lỗi'))) {
                    activateLeafletFallback(centerPos);
                }
            }, 2500);

            return;
        } catch (err) {
            console.warn('Google Maps initialization failed, switching to backup map:', err);
            googleMapsAuthFailed = true;
            activateLeafletFallback(centerPos);
        }
    }

    // Activate Fallback Interactive Map (Leaflet / OpenStreetMap)
    function activateLeafletFallback(centerPos) {
        fallbackNoticeEl = document.getElementById('map-fallback-notice');
        if (fallbackNoticeEl) fallbackNoticeEl.hidden = false;

        // Clean mapSearchInput to remove Google Maps broken autocomplete error state
        const currentInput = document.getElementById('map-search-input');
        if (currentInput) {
            const cleanInput = currentInput.cloneNode(false);
            cleanInput.id = 'map-search-input';
            cleanInput.className = currentInput.className;
            cleanInput.placeholder = 'Tìm kiếm địa chỉ, tên đường trên bản đồ...';
            cleanInput.value = '';
            cleanInput.disabled = false;
            currentInput.parentNode.replaceChild(cleanInput, currentInput);

            cleanInput.addEventListener('keydown', async (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    const q = cleanInput.value.trim();
                    if (!q) return;
                    if (mapCurrentAddress) mapCurrentAddress.innerHTML = '<i class="fa fa-spinner fa-spin"></i> Đang tìm kiếm...';
                    try {
                        const res = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(q)}&countrycodes=vn&limit=1&accept-language=vi`);
                        if (res.ok) {
                            const data = await res.json();
                            if (data && data[0]) {
                                const lat = parseFloat(data[0].lat);
                                const lng = parseFloat(data[0].lon);
                                if (activeLeafletMap && activeLeafletMarker) {
                                    activeLeafletMap.setView([lat, lng], 17);
                                    activeLeafletMarker.setLatLng([lat, lng]);
                                }
                                updateModalAddressDisplay(data[0].display_name, lat, lng);
                                return;
                            }
                        }
                        notify('Không tìm thấy địa điểm này. Vui lòng thử từ khóa khác.', true);
                    } catch (err) {
                        console.warn('Search error:', err);
                    }
                }
            });
        }

        renderLeafletMap(centerPos || selectedLocation);
    }

    function renderLeafletMap(centerPos) {
        if (!window.L) {
            console.error('Leaflet is not available on window');
            return;
        }

        mapContainer.replaceChildren();
        const mapDiv = document.createElement('div');
        mapDiv.style.width = '100%';
        mapDiv.style.height = '100%';
        mapContainer.appendChild(mapDiv);

        if (activeLeafletMap) {
            try { activeLeafletMap.remove(); } catch (e) {}
        }

        activeLeafletMap = window.L.map(mapDiv).setView([centerPos.lat, centerPos.lng], 16);
        window.L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '© OpenStreetMap contributors'
        }).addTo(activeLeafletMap);

        // Explicit Leaflet marker icons for crisp rendering
        const leafletIcon = window.L.icon({
            iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
            iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
            shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
            iconSize: [25, 41],
            iconAnchor: [12, 41],
            popupAnchor: [1, -34],
            shadowSize: [41, 41]
        });

        activeLeafletMarker = window.L.marker([centerPos.lat, centerPos.lng], {
            icon: leafletIcon,
            draggable: true
        }).addTo(activeLeafletMap);

        activeLeafletMarker.on('dragend', () => {
            const pos = activeLeafletMarker.getLatLng();
            onLocationMoved(pos.lat, pos.lng);
        });

        activeLeafletMap.on('click', (e) => {
            activeLeafletMarker.setLatLng(e.latlng);
            onLocationMoved(e.latlng.lat, e.latlng.lng);
        });

        setTimeout(() => {
            if (activeLeafletMap) activeLeafletMap.invalidateSize();
        }, 200);

        onLocationMoved(centerPos.lat, centerPos.lng);
    }

    // Modal Events
    if (btnOpenMap) {
        btnOpenMap.addEventListener('click', openMapModal);
    }
    if (btnModalClose) {
        btnModalClose.addEventListener('click', closeMapModal);
    }
    if (btnModalCancel) {
        btnModalCancel.addEventListener('click', closeMapModal);
    }
    if (mapModal) {
        mapModal.addEventListener('click', (e) => {
            if (e.target === mapModal) closeMapModal();
        });
    }

    // Confirm Address from Modal
    if (btnModalConfirm) {
        btnModalConfirm.addEventListener('click', () => {
            if (selectedLocation.address && shippingAddressInput) {
                shippingAddressInput.value = selectedLocation.address;
                highlightAddressInput();
                notify('Đã cập nhật địa chỉ giao hàng từ bản đồ!');
            }
            closeMapModal();
        });
    }

    // GPS inside Modal
    if (btnModalGps) {
        btnModalGps.addEventListener('click', () => {
            if (!navigator.geolocation) {
                notify('Trình duyệt không hỗ trợ GPS.', true);
                return;
            }
            btnModalGps.disabled = true;
            btnModalGps.innerHTML = '<i class="fa fa-spinner fa-spin"></i> Đang định vị...';

            navigator.geolocation.getCurrentPosition(
                (pos) => {
                    btnModalGps.disabled = false;
                    btnModalGps.innerHTML = '<i class="fa fa-crosshairs"></i> Vị trí của tôi';
                    const lat = pos.coords.latitude;
                    const lng = pos.coords.longitude;

                    if (activeMap && activeMarker && !googleMapsAuthFailed) {
                        activeMap.setCenter({ lat, lng });
                        activeMap.setZoom(17);
                        activeMarker.setPosition({ lat, lng });
                    } else if (activeLeafletMap && activeLeafletMarker) {
                        activeLeafletMap.setView([lat, lng], 17);
                        activeLeafletMarker.setLatLng([lat, lng]);
                    }
                    onLocationMoved(lat, lng);
                },
                (err) => {
                    btnModalGps.disabled = false;
                    btnModalGps.innerHTML = '<i class="fa fa-crosshairs"></i> Vị trí của tôi';
                    notify('Không thể lấy vị trí GPS hiện tại: ' + err.message, true);
                },
                { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
            );
        });
    }

    // Pre-load Google Maps if key is present
    if (googleMapsApiKey) {
        loadGoogleMapsScript().catch(err => {
            console.log('Google Maps API key background preload note:', err.message);
        });
    }
})();
