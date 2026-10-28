package com.example.shoptech.dto;

import java.time.OffsetDateTime;

/** Only storefront information; application contacts and owner data stay private. */
public record PublicShop(Long id, String name, String slug, String description, OffsetDateTime joinedAt) {}
