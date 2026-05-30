package com.library.system.controller;

import com.library.system.base.ControllerTestBase;
import com.library.system.dto.PageResult;
import com.library.system.dto.VolunteerResponse;
import com.library.system.service.VolunteerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("VolunteerController 测试")
class VolunteerControllerTest extends ControllerTestBase {

    @Mock
    private VolunteerService volunteerService;

    @InjectMocks
    private VolunteerController volunteerController;

    @BeforeEach
    void setUp() {
        initMockMvc(volunteerController);
    }

    @Nested
    @DisplayName("查询接口")
    class QueryEndpoints {

        @Test
        @DisplayName("获取志愿服务列表 - 应返回200")
        void listVolunteers_shouldReturn200() throws Exception {
            PageResult<VolunteerResponse> pageResult = new PageResult<>();
            pageResult.setTotal(0L);
            pageResult.setRecords(List.of());
            when(volunteerService.listVolunteers(anyLong(), anyLong(), any())).thenReturn(pageResult);

            mockMvc.perform(get("/volunteers")
                    .param("current", "1")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        @Test
        @DisplayName("获取志愿服务详情 - 应返回200")
        void getVolunteerById_shouldReturn200() throws Exception {
            VolunteerResponse response = new VolunteerResponse();
            response.setId(1L);
            when(volunteerService.getVolunteerById(1L)).thenReturn(response);

            mockMvc.perform(get("/volunteers/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }
}
