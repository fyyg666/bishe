package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.system.dto.BranchRequest;
import com.library.system.dto.BranchResponse;
import com.library.system.entity.Book;
import com.library.system.entity.Branch;
import com.library.system.entity.ReadingRoom;
import com.library.system.entity.Seat;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.BookMapper;
import com.library.system.mapper.BranchMapper;
import com.library.system.mapper.ReadingRoomMapper;
import com.library.system.mapper.SeatMapper;
import com.library.system.service.BranchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final BranchMapper branchMapper;
    private final BookMapper bookMapper;
    private final SeatMapper seatMapper;
    private final ReadingRoomMapper readingRoomMapper;

    @Override
    public List<BranchResponse> listBranches(String status) {
        LambdaQueryWrapper<Branch> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Branch::getStatus, Integer.valueOf(status));
        }
        wrapper.orderByAsc(Branch::getId);

        List<Branch> branches = branchMapper.selectList(wrapper);
        Map<Long, String> branchMap = branches.stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getName, (a, b) -> a));

        Map<Long, Long> childrenCountMap = branches.stream()
                .filter(b -> b.getParentId() != null)
                .collect(Collectors.groupingBy(Branch::getParentId, Collectors.counting()));

        return branches.stream().map(b -> toResponse(b, branchMap, childrenCountMap)).collect(Collectors.toList());
    }

    @Override
    public List<BranchResponse> listBranchTree() {
        List<BranchResponse> flatList = listBranches(null);
        Map<Long, BranchResponse> nodeMap = flatList.stream()
                .collect(Collectors.toMap(BranchResponse::getId, b -> b, (a, b) -> a));

        List<BranchResponse> roots = new ArrayList<>();
        for (BranchResponse node : flatList) {
            if (node.getParentId() == null) {
                roots.add(node);
            } else {
                BranchResponse parent = nodeMap.get(node.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        return roots;
    }

    @Override
    public BranchResponse getBranch(Long id) {
        Branch branch = branchMapper.selectById(id);
        if (branch == null || branch.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "分馆不存在");
        }

        BranchResponse response = BranchResponse.builder()
                .id(branch.getId())
                .name(branch.getName())
                .code(branch.getCode())
                .address(branch.getAddress())
                .phone(branch.getPhone())
                .email(branch.getEmail())
                .openingHours(branch.getOpeningHours())
                .status(branch.getStatus())
                .parentId(branch.getParentId())
                .createTime(branch.getCreateTime())
                .updateTime(branch.getUpdateTime())
                .build();

        if (branch.getParentId() != null) {
            Branch parent = branchMapper.selectById(branch.getParentId());
            if (parent != null) {
                response.setParentName(parent.getName());
            }
        }

        Long childCount = branchMapper.selectCount(
                new LambdaQueryWrapper<Branch>().eq(Branch::getParentId, id));
        response.setChildrenCount(childCount.intValue());

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BranchResponse createBranch(BranchRequest request) {
        Long existing = branchMapper.selectCount(
                new LambdaQueryWrapper<Branch>().eq(Branch::getCode, request.getCode()));
        if (existing > 0) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "分馆编码已存在: " + request.getCode());
        }

        Branch branch = Branch.builder()
                .name(request.getName())
                .code(request.getCode())
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .openingHours(request.getOpeningHours())
                .status(request.getStatus() != null ? request.getStatus() : 1)
                .parentId(request.getParentId())
                .build();

        branchMapper.insert(branch);
        log.info("分馆创建成功: {}", branch.getName());
        return getBranch(branch.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BranchResponse updateBranch(Long id, BranchRequest request) {
        Branch existing = branchMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "分馆不存在");
        }

        if (!existing.getCode().equals(request.getCode())) {
            Long count = branchMapper.selectCount(
                    new LambdaQueryWrapper<Branch>().eq(Branch::getCode, request.getCode()));
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMETER_ERROR, "分馆编码已存在: " + request.getCode());
            }
        }

        existing.setName(request.getName());
        existing.setCode(request.getCode());
        existing.setAddress(request.getAddress());
        existing.setPhone(request.getPhone());
        existing.setEmail(request.getEmail());
        existing.setOpeningHours(request.getOpeningHours());
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        existing.setParentId(request.getParentId());

        branchMapper.updateById(existing);
        log.info("分馆更新成功: id={}", id);
        return getBranch(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBranch(Long id) {
        Branch existing = branchMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "分馆不存在");
        }

        Long childCount = branchMapper.selectCount(
                new LambdaQueryWrapper<Branch>().eq(Branch::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "该分馆下存在子分馆，无法删除");
        }

        Long bookCount = bookMapper.selectCount(
                new LambdaQueryWrapper<Book>().eq(Book::getBranchId, id));
        if (bookCount > 0) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "该分馆下存在图书数据，无法删除");
        }

        Long seatCount = seatMapper.selectCount(
                new LambdaQueryWrapper<Seat>().eq(Seat::getBranchId, id));
        if (seatCount > 0) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "该分馆下存在座位数据，无法删除");
        }

        Long roomCount = readingRoomMapper.selectCount(
                new LambdaQueryWrapper<ReadingRoom>().eq(ReadingRoom::getBranchId, id));
        if (roomCount > 0) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "该分馆下存在阅览室数据，无法删除");
        }

        branchMapper.deleteById(id);
        log.info("分馆删除成功: id={}", id);
    }

    @Override
    public List<BranchResponse> getSubBranches(Long parentId) {
        LambdaQueryWrapper<Branch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Branch::getParentId, parentId);
        wrapper.orderByAsc(Branch::getId);

        List<Branch> branches = branchMapper.selectList(wrapper);
        Map<Long, Long> childrenCountMap = branches.stream()
                .filter(b -> b.getParentId() != null)
                .collect(Collectors.groupingBy(Branch::getParentId, Collectors.counting()));

        Map<Long, String> branchMap = branches.stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getName, (a, b) -> a));

        Branch parent = branchMapper.selectById(parentId);
        if (parent != null) {
            branchMap.put(parent.getId(), parent.getName());
        }

        return branches.stream().map(b -> toResponse(b, branchMap, childrenCountMap)).collect(Collectors.toList());
    }

    private BranchResponse toResponse(Branch branch, Map<Long, String> branchMap, Map<Long, Long> childrenCountMap) {
        return BranchResponse.builder()
                .id(branch.getId())
                .name(branch.getName())
                .code(branch.getCode())
                .address(branch.getAddress())
                .phone(branch.getPhone())
                .email(branch.getEmail())
                .openingHours(branch.getOpeningHours())
                .status(branch.getStatus())
                .parentId(branch.getParentId())
                .parentName(branch.getParentId() != null ? branchMap.get(branch.getParentId()) : null)
                .childrenCount(childrenCountMap.getOrDefault(branch.getId(), 0L).intValue())
                .createTime(branch.getCreateTime())
                .updateTime(branch.getUpdateTime())
                .build();
    }
}
