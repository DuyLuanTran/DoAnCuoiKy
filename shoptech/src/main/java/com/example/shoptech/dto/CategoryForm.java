package com.example.shoptech.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoryForm {

    @NotBlank(message = "Tên danh mục không được để trống.")
    @Size(max = 100, message = "Tên danh mục tối đa 100 ký tự.")
    private String name;

    @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự.")
    private String description;

    @Size(max = 50, message = "Class biểu tượng tối đa 50 ký tự.")
    private String iconClass;

    private boolean active = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.strip();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null
                ? null
                : description.strip();
    }

    public String getIconClass() {
        return iconClass;
    }

    public void setIconClass(String iconClass) {
        this.iconClass = iconClass == null
                ? null
                : iconClass.strip();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}