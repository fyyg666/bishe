package com.library.system.service;

import com.library.system.dto.ReaderRequest;
import com.library.system.dto.ReaderResponse;
import com.library.system.entity.Reader;
import com.library.system.mapper.ReaderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 读者服务单元测试
 *
 * @author Library Team
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class ReaderServiceTest {

    @Mock
    private ReaderMapper readerMapper;

    @InjectMocks
    private ReaderServiceImpl readerService;

    private Reader testReader;
    private ReaderRequest testRequest;

    @BeforeEach
    void setUp() {
        testReader = new Reader();
        testReader.setId(1L);
        testReader.setUsername("testreader");
        testReader.setRealName("张三");
        testReader.setCardNumber("R2024010001");
        testReader.setPhone("13800138000");
        testReader.setEmail("zhangsan@example.com");
        testReader.setCreditScore(100);
        testReader.setStatus(1);
        testReader.setMaxBorrowCount(5);

        testRequest = new ReaderRequest();
        testRequest.setUsername("newreader");
        testRequest.setPassword("password123");
        testRequest.setRealName("李四");
        testRequest.setPhone("13900139000");
        testRequest.setEmail("lisi@example.com");
    }

    @Test
    void testGetReaderById_Success() {
        when(readerMapper.selectById(1L)).thenReturn(testReader);

        ReaderResponse result = readerService.getReaderById(1L);

        assertNotNull(result);
        assertEquals("testreader", result.getUsername());
        assertEquals("张三", result.getRealName());
        assertEquals(100, result.getCreditScore());
        verify(readerMapper).selectById(1L);
    }

    @Test
    void testGetReaderById_NotFound() {
        when(readerMapper.selectById(999L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> readerService.getReaderById(999L));
        verify(readerMapper).selectById(999L);
    }

    @Test
    void testCreateReader_Success() {
        when(readerMapper.insert(any(Reader.class))).thenReturn(1);

        ReaderResponse result = readerService.createReader(testRequest);

        assertNotNull(result);
        assertEquals("newreader", result.getUsername());
        assertEquals("李四", result.getRealName());
        verify(readerMapper).insert(any(Reader.class));
    }

    @Test
    void testCreateReader_DuplicateUsername() {
        when(readerMapper.selectByUsername("newreader")).thenReturn(testReader);

        assertThrows(RuntimeException.class, () -> readerService.createReader(testRequest));
        verify(readerMapper, never()).insert(any(Reader.class));
    }

    @Test
    void testUpdateReader_Success() {
        when(readerMapper.selectById(1L)).thenReturn(testReader);
        when(readerMapper.updateById(any(Reader.class))).thenReturn(1);

        testRequest.setRealName("张三更新");
        testRequest.setPhone("13800138001");

        ReaderResponse result = readerService.updateReader(1L, testRequest);

        assertNotNull(result);
        verify(readerMapper).updateById(any(Reader.class));
    }

    @Test
    void testUpdateReader_NotFound() {
        when(readerMapper.selectById(999L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> readerService.updateReader(999L, testRequest));
        verify(readerMapper, never()).updateById(any(Reader.class));
    }

    @Test
    void testDeleteReader_Success() {
        when(readerMapper.selectById(1L)).thenReturn(testReader);
        doNothing().when(readerMapper).deleteById(anyLong());

        assertDoesNotThrow(() -> readerService.deleteReader(1L));
        verify(readerMapper).deleteById(1L);
    }

    @Test
    void testDeleteReader_NotFound() {
        when(readerMapper.selectById(999L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> readerService.deleteReader(999L));
        verify(readerMapper, never()).deleteById(anyLong());
    }

    @Test
    void testGetAllReaders_Success() {
        List<Reader> readers = Arrays.asList(testReader);
        when(readerMapper.selectList(null)).thenReturn(readers);

        List<ReaderResponse> result = readerService.getAllReaders();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testreader", result.get(0).getUsername());
        verify(readerMapper).selectList(null);
    }

    @Test
    void testSearchReaders_Success() {
        List<Reader> readers = Arrays.asList(testReader);
        when(readerMapper.searchByKeyword("张三")).thenReturn(readers);

        List<ReaderResponse> result = readerService.searchReaders("张三");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(readerMapper).searchByKeyword("张三");
    }

    @Test
    void testUpdateCreditScore_Success() {
        when(readerMapper.selectById(1L)).thenReturn(testReader);
        when(readerMapper.updateById(any(Reader.class))).thenReturn(1);

        ReaderResponse result = readerService.updateCreditScore(1L, 10, "借书奖励");

        assertNotNull(result);
        verify(readerMapper).updateById(any(Reader.class));
    }

    @Test
    void testUpdateCreditScore_NotFound() {
        when(readerMapper.selectById(999L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> readerService.updateCreditScore(999L, 10, "借书奖励"));
        verify(readerMapper, never()).updateById(any(Reader.class));
    }

    @Test
    void testGetReaderByCardNumber_Success() {
        when(readerMapper.selectByCardNumber("R2024010001")).thenReturn(testReader);

        ReaderResponse result = readerService.getReaderByCardNumber("R2024010001");

        assertNotNull(result);
        assertEquals("R2024010001", result.getCardNumber());
        verify(readerMapper).selectByCardNumber("R2024010001");
    }

    @Test
    void testGetReaderByCardNumber_NotFound() {
        when(readerMapper.selectByCardNumber("R9999999999")).thenReturn(null);

        assertThrows(RuntimeException.class, () -> readerService.getReaderByCardNumber("R9999999999"));
        verify(readerMapper).selectByCardNumber("R9999999999");
    }
}
