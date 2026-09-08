package com.tuyou.controller;

import com.tuyou.common.Result;
import com.tuyou.service.DestinationService;
import com.tuyou.vo.DestinationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 目的地接口
 */
@RestController
@RequestMapping("/api/destinations")
@RequiredArgsConstructor
public class DestinationController {

    private final DestinationService destinationService;

    @GetMapping
    public Result<List<DestinationVO>> list() {
        return Result.ok(destinationService.listAll());
    }
}
