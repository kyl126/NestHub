package com.openisle.controller;

import com.openisle.dto.SubChannelDto;
import com.openisle.service.SubChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sub-channels")
public class SubChannelController {

    @Autowired
    private SubChannelService subChannelService;

    @GetMapping("/by-channel/{channelId}")
    public List<SubChannelDto> getByChannelId(@PathVariable Long channelId) {
        return subChannelService.findByCategoryId(channelId);
    }
}