package com.tuyou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuyou.entity.Destination;
import com.tuyou.mapper.DestinationMapper;
import com.tuyou.vo.DestinationVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 目的地服务单元测试（W3D4 补空白：Destination 此前 0 覆盖）
 */
@ExtendWith(MockitoExtension.class)
class DestinationServiceImplTest {

    @Mock
    private DestinationMapper destinationMapper;

    @InjectMocks
    private DestinationServiceImpl destinationService;

    @Test
    @DisplayName("目的地列表：映射为 VO")
    void listAll() {
        Destination d = new Destination();
        d.setId(1L);
        d.setName("桂林");
        d.setPinyin("guilin");
        d.setHotFlag(1);
        when(destinationMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(d));

        List<DestinationVO> list = destinationService.listAll();
        assertEquals(1, list.size());
        assertEquals("桂林", list.get(0).getName());
        assertEquals("guilin", list.get(0).getPinyin());
    }

    @Test
    @DisplayName("目的地列表：空表返回空列表")
    void listAllEmpty() {
        when(destinationMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        assertTrue(destinationService.listAll().isEmpty());
    }
}
