package com.example.shoptech.controller;

import com.example.shoptech.service.StoredOrderNotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@Controller @RequestMapping("/notifications") @PreAuthorize("isAuthenticated()")
public class OrderNotificationController {
    private final StoredOrderNotificationService notifications;
    public OrderNotificationController(StoredOrderNotificationService notifications) { this.notifications = notifications; }
    @GetMapping @ResponseBody public StoredOrderNotificationService.Inbox inbox(Principal user,@RequestParam(defaultValue="0") int page) { return notifications.inbox(user.getName(),page); }
    @PostMapping("/{id}/open") public String open(Principal user, @PathVariable Long id) {
        return "redirect:" + notifications.open(user.getName(), id);
    }
}
