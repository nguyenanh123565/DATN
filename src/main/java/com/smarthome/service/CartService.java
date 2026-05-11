package com.smarthome.service;

import com.smarthome.common.BusinessException;
import com.smarthome.common.ResourceNotFoundException;
import com.smarthome.dto.CartDto;
import com.smarthome.entity.Cart;
import com.smarthome.entity.CartItem;
import com.smarthome.entity.Product;
import com.smarthome.entity.User;
import com.smarthome.repository.CartRepository;
import com.smarthome.repository.ProductRepository;
import com.smarthome.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class CartService {
    private final CartRepository cartRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    public CartService(CartRepository cartRepo, ProductRepository productRepo, UserRepository userRepo) {
        this.cartRepo = cartRepo;
        this.productRepo = productRepo;
        this.userRepo = userRepo;
    }

    public CartDto.Response getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return toResponse(cart);
    }

    public CartDto.Response addItem(Long userId, CartDto.AddItemRequest req) {
        Cart cart = getOrCreateCart(userId);
        Product product = productRepo.findById(req.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", req.getProductId()));

        if (!product.getIsActive()) throw new BusinessException("Sản phẩm không còn bán!");
        if (product.getStockQuantity() < req.getQuantity())
            throw new BusinessException("Sản phẩm không đủ hàng! Còn lại: " + product.getStockQuantity());

        // Kiểm tra item đã có trong giỏ chưa
        CartItem existing = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(req.getProductId()))
                .findFirst().orElse(null);

        if (existing != null) {
            int newQty = existing.getQuantity() + req.getQuantity();
            if (newQty > product.getStockQuantity())
                throw new BusinessException("Giỏ hàng vượt quá tồn kho! Tối đa: " + product.getStockQuantity());
            existing.setQuantity(newQty);
        } else {
            BigDecimal price = product.getSalePrice() != null ? product.getSalePrice() : product.getPrice();
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(req.getQuantity());
            item.setUnitPrice(price);
            cart.getItems().add(item);
        }

        return toResponse(cartRepo.save(cart));
    }

    public CartDto.Response updateItem(Long userId, CartDto.UpdateItemRequest req) {
        Cart cart = getOrCreateCart(userId);

        if (req.getQuantity() == 0) {
            cart.getItems().removeIf(i -> i.getProduct().getId().equals(req.getProductId()));
        } else {
            Product p = productRepo.findById(req.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", req.getProductId()));
            if (req.getQuantity() > p.getStockQuantity())
                throw new BusinessException("Không đủ hàng! Tồn kho: " + p.getStockQuantity());

            cart.getItems().stream()
                    .filter(i -> i.getProduct().getId().equals(req.getProductId()))
                    .findFirst()
                    .ifPresent(i -> i.setQuantity(req.getQuantity()));
        }

        return toResponse(cartRepo.save(cart));
    }

    public CartDto.Response removeItem(Long userId, Long productId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().removeIf(i -> i.getProduct().getId().equals(productId));
        return toResponse(cartRepo.save(cart));
    }

    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cartRepo.save(cart);
    }

    public BigDecimal calculateSubtotal(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return cart.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ─── Private helpers ──────────────────────────────────────────────

    private Cart getOrCreateCart(Long userId) {
        return cartRepo.findByUserId(userId).orElseGet(() -> {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));
            Cart newCart = new Cart();
            newCart.setUser(user);
            return cartRepo.save(newCart);
        });
    }

    private CartDto.Response toResponse(Cart cart) {
        List<CartDto.CartItemResponse> itemsRes = cart.getItems().stream().map(i -> {
            CartDto.CartItemResponse ir = new CartDto.CartItemResponse();
            ir.setId(i.getId());
            ir.setProductId(i.getProduct().getId());
            ir.setProductName(i.getProduct().getName());
            ir.setUnitPrice(i.getUnitPrice());
            ir.setQuantity(i.getQuantity());
            ir.setSubtotal(i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())));
            ir.setStockQuantity(i.getProduct().getStockQuantity());
            // Lấy ảnh đầu tiên từ imageUrls JSON
            try {
                if (i.getProduct().getImageUrls() != null) {
                    String imgs = i.getProduct().getImageUrls();
                    if (imgs.startsWith("[")) {
                        imgs = imgs.replaceAll("[\\[\\]\"]", "").split(",")[0].trim();
                    }
                    ir.setProductImage(imgs);
                }
            } catch (Exception ignored) {}
            return ir;
        }).toList();

        BigDecimal total = itemsRes.stream()
                .map(CartDto.CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CartDto.Response res = new CartDto.Response();
        res.setId(cart.getId());
        res.setItems(itemsRes);
        res.setTotalItems(itemsRes.stream().mapToInt(CartDto.CartItemResponse::getQuantity).sum());
        res.setTotalAmount(total);
        return res;
    }
}
