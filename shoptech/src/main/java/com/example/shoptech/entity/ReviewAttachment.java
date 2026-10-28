package com.example.shoptech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Embeddable @Getter @Setter
public class ReviewAttachment {
    @Column(nullable=false,length=1000) private String url;
    @Column(nullable=false,length=255) private String storageId;
    @Column(nullable=false,length=10) private String kind;
    public ReviewAttachment() {}
    public ReviewAttachment(String url,String storageId,String kind){this.url=url;this.storageId=storageId;this.kind=kind;}
}
