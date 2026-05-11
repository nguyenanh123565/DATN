package com.smarthome.service;

import com.smarthome.common.BusinessException;
import com.smarthome.common.ResourceNotFoundException;
import com.smarthome.dto.ReviewDto;
import com.smarthome.entity.Product;
import com.smarthome.entity.ProductReview;
import com.smarthome.entity.User;
import com.smarthome.repository.ProductRepository;
import com.smarthome.repository.ReviewRepository;
import com.smarthome.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    public ReviewService(ReviewRepository reviewRepo, ProductRepository productRepo, UserRepository userRepo) {
        this.reviewRepo = reviewRepo;
        this.productRepo = productRepo;
        this.userRepo = userRepo;
    }

    public Page<ReviewDto.Response> getByProduct(Long productId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return reviewRepo.findByProductIdAndIsApprovedTrue(productId, pageable).map(this::toResponse);
    }

    @Transactional
    public ReviewDto.Response create(ReviewDto.CreateRequest req, Long userId) {
        if (reviewRepo.existsByProductIdAndUserId(req.getProductId(), userId)) {
            throw new BusinessException("Bạn đã đánh giá sản phẩm này rồi!");
        }

        Product product = productRepo.findById(req.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", req.getProductId()));

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(req.getRating());
        review.setComment(req.getComment());
        review.setIsApproved(true); // Auto approve; có thể đổi thành false nếu cần duyệt

        return toResponse(reviewRepo.save(review));
    }

    @Transactional
    public void approve(Long reviewId) {
        ProductReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));
        review.setIsApproved(true);
        reviewRepo.save(review);
    }

    @Transactional
    public void delete(Long reviewId) {
        reviewRepo.deleteById(reviewId);
    }

    private ReviewDto.Response toResponse(ProductReview r) {
        ReviewDto.Response res = new ReviewDto.Response();
        res.setId(r.getId());
        res.setProductId(r.getProduct().getId());
        res.setUserName(r.getUser().getFullName());
        res.setRating(r.getRating());
        res.setComment(r.getComment());
        res.setIsApproved(r.getIsApproved());
        res.setCreatedAt(r.getCreatedAt());
        return res;
    }
}
