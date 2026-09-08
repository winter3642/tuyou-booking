package com.tuyou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuyou.entity.Destination;
import com.tuyou.mapper.DestinationMapper;
import com.tuyou.service.DestinationService;
import com.tuyou.vo.DestinationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DestinationServiceImpl implements DestinationService {

    private final DestinationMapper destinationMapper;

    @Override
    public List<DestinationVO> listAll() {
        List<Destination> list = destinationMapper.selectList(
                new LambdaQueryWrapper<Destination>()
                        .orderByDesc(Destination::getHotFlag)
                        .orderByAsc(Destination::getId));
        return list.stream().map(d -> {
            DestinationVO vo = new DestinationVO();
            BeanUtils.copyProperties(d, vo);
            return vo;
        }).collect(Collectors.toList());
    }
}
