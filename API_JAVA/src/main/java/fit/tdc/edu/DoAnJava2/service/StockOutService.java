package fit.tdc.edu.DoAnJava2.service;

import fit.tdc.edu.DoAnJava2.dto.StockOutItemRequest;
import fit.tdc.edu.DoAnJava2.dto.StockOutRequest;
import fit.tdc.edu.DoAnJava2.model.StockOut;
import fit.tdc.edu.DoAnJava2.model.StockOutItem;
import fit.tdc.edu.DoAnJava2.repository.StockOutItemRepository;
import fit.tdc.edu.DoAnJava2.repository.StockOutRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import fit.tdc.edu.DoAnJava2.repository.UserRepository;
import fit.tdc.edu.DoAnJava2.model.User;
import fit.tdc.edu.DoAnJava2.model.Product;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockOutService {

    @Autowired
    private StockOutRepository stockOutRepository;

    @Autowired
    private StockOutItemRepository stockOutItemRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserRepository userRepository;

    public List<StockOut> getAllStockOuts() {
        return stockOutRepository.findAll();
    }

    public List<StockOutItem> getStockOutItems(Long stockOutId) {
        return stockOutItemRepository.findByStockOutId(stockOutId);
    }

    public StockOut getById(Long id) {
        StockOut stockOut = stockOutRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu xuất với ID: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        boolean isAdmin = false;
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin) {
                User user = userRepository.findByUsername(auth.getName()).orElse(null);
                if (user != null && !stockOut.getUserId().equals(user.getId())) {
                    throw new RuntimeException("Lỗi: Bạn không có quyền truy cập phiếu xuất này!");
                }
            }
        }
        return stockOut;
    }

    // LẬP PHIẾU XUẤT MỚI: Mặc định là PENDING và KHÔNG trừ kho ngay
    @Transactional
    public StockOut createStockOut(StockOutRequest request) {
        if (request == null) {
            throw new RuntimeException("Lỗi: Dữ liệu yêu cầu không hợp lệ!");
        }
        if (request.getCustomerId() == null) {
            throw new RuntimeException("Lỗi: Vui lòng chọn đối tác khách hàng!");
        }
        if (request.getUserId() == null) {
            throw new RuntimeException("Lỗi: Không tìm thấy thông tin người dùng lập phiếu!");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Lỗi: Phiếu xuất chưa có mặt hàng nào!");
        }

        // 1. Kiểm tra tồn kho trước khi cho phép khởi tạo phiếu
        for (StockOutItemRequest itemReq : request.getItems()) {
            if (itemReq.getProductId() == null) {
                throw new RuntimeException("Lỗi: Mã sản phẩm không hợp lệ!");
            }
            if (itemReq.getQuantity() <= 0) {
                throw new RuntimeException("Lỗi: Số lượng xuất của từng sản phẩm phải lớn hơn 0!");
            }
            if (itemReq.getPrice() < 0) {
                throw new RuntimeException("Lỗi: Đơn giá xuất của từng sản phẩm không được nhỏ hơn 0!");
            }

            Product product = productService.getProductById(itemReq.getProductId());
            if (product == null) {
                throw new RuntimeException("Sản phẩm với ID " + itemReq.getProductId() + " không tồn tại!");
            }
            if (product.getQuantity() < itemReq.getQuantity()) {
                throw new RuntimeException("Lỗi: Số lượng tồn kho của sản phẩm '" + product.getName() +
                        "' không đủ (Còn: " + product.getQuantity() + ", Yêu cầu: " + itemReq.getQuantity() + ")");
            }
        }

        // 2. Khởi tạo phiếu xuất nếu tất cả sản phẩm đều đủ tồn kho
        StockOut stockOut = new StockOut();
        stockOut.setCustomerId(request.getCustomerId());
        stockOut.setUserId(request.getUserId());
        stockOut.setCreatedAt(LocalDateTime.now());
        stockOut.setStatus("PENDING");
        StockOut savedStockOut = stockOutRepository.save(stockOut);

        for (StockOutItemRequest itemReq : request.getItems()) {
            StockOutItem item = new StockOutItem();
            item.setStockOutId(savedStockOut.getId());
            item.setProductId(itemReq.getProductId());
            item.setQuantity(itemReq.getQuantity());
            item.setPrice(itemReq.getPrice());
            stockOutItemRepository.save(item);
        }
        return savedStockOut;
    }

    // DUYỆT HOẶC HỦY PHIẾU XUẤT KHO: Thực hiện trừ tồn kho nếu COMPLETED
    @Transactional
    public StockOut changeStatus(Long id, String newStatus) {
        StockOut stockOut = stockOutRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu xuất!"));

        if (!stockOut.getStatus().equals("PENDING")) {
            throw new RuntimeException("Phiếu này đã được xử lý từ trước, không thể đổi trạng thái!");
        }

        stockOut.setStatus(newStatus.toUpperCase());

        // NẾU DUYỆT THÌ MỚI CHÍNH THỨC TRỪ TỒN KHO
        if (newStatus.equalsIgnoreCase("COMPLETED")) {
            List<StockOutItem> items = stockOutItemRepository.findByStockOutId(id);
            for (StockOutItem item : items) {
                // Sử dụng hàm nghiệp vụ mới để trừ kho, tăng tổng bán và tính giá xuất trung bình
                productService.updateStockAndExportPrice(item.getProductId(), item.getQuantity(), item.getPrice());
            }
        }
        return stockOutRepository.save(stockOut);
    }

    // 🌟 PHÂN TRANG VÀ TÌM KIẾM PHIẾU XUẤT KHO KẾT HỢP PHÂN QUYỀN
    public Page<StockOut> getStockOutsWithPage(String keyword, int page, int size, String sortDir) {
        // 1. Cấu hình phân trang và thứ tự sắp xếp theo ID phiếu (Tăng dần hoặc giảm dần)
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by("id").descending() : Sort.by("id").ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // 2. Lấy thông tin tài khoản đăng nhập hiện tại từ Security Context (JWT Token)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = false;
        Long currentUserId = null;

        // 3. Xử lý phân quyền: Xác định vai trò người dùng (ADMIN hoặc STAFF)
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            // Kiểm tra xem danh sách quyền của người dùng có chứa quyền quản trị 'ROLE_ADMIN' hay không
            isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            
            // Nếu là STAFF (Nhân viên thường) -> Truy vấn CSDL để lấy ID người dùng tương ứng với username
            if (!isAdmin) {
                User user = userRepository.findByUsername(auth.getName()).orElse(null);
                if (user != null) {
                    currentUserId = user.getId(); // Lưu trữ ID nhân viên để lọc dữ liệu về sau
                } else {
                    isAdmin = true; // Fallback: Nếu không tìm thấy user, cho phép quyền Admin để tránh mất dữ liệu
                }
            }
        } else {
            isAdmin = true; // Fallback: Cho phép truy cập
        }

        // 4. TRƯỜNG HỢP 1: Không có từ khóa tìm kiếm -> Lấy toàn bộ danh sách (Có phân quyền)
        if (keyword == null || keyword.trim().isEmpty()) {
            if (isAdmin) {
                return stockOutRepository.findAll(pageable); // ADMIN: Xem toàn bộ phiếu xuất của hệ thống
            } else {
                return stockOutRepository.findByUserId(currentUserId, pageable); // STAFF: Chỉ xem phiếu do chính mình lập
            }
        }

        String rawKeyword = keyword.trim();

        // 5. TRƯỜNG HỢP 2: Người dùng gõ tìm kiếm đích danh ID phiếu dạng "#PX-1" hoặc "PX1"
        if (rawKeyword.toUpperCase().startsWith("#PX-") || rawKeyword.toUpperCase().startsWith("PX")) {
            try {
                // Sử dụng Regex để lọc bỏ chữ ký hiệu, chỉ lấy phần số nguyên ID (Ví dụ: "#PX-12" -> "12")
                Long exactId = Long.parseLong(rawKeyword.replaceAll("(?i)^#?PX-?", ""));
                if (isAdmin) {
                    return stockOutRepository.findByExactId(exactId, pageable); // ADMIN: Tìm kiếm trên toàn hệ thống
                } else {
                    return stockOutRepository.findByExactIdAndUserId(exactId, currentUserId, pageable); // STAFF: Tìm ID nhưng chỉ trong phiếu của mình
                }
            } catch (NumberFormatException e) {
                return Page.empty(pageable); // Trả về trang trống nếu định dạng số không hợp lệ
            }
        }

        // 6. TRƯỜNG HỢP 3: Tìm kiếm tự do theo từ khóa (Tên khách hàng, Tên người lập, Trạng thái...)
        // Dịch từ khóa trạng thái tiếng Việt sang tiếng Anh tương ứng trong Cơ sở dữ liệu (Database)
        String searchStatus = "UNKNOWN_STATUS"; // Mặc định gán trạng thái ảo để chặn hiển thị nhầm lẫn
        String cleanKeyword = rawKeyword;
        
        if (cleanKeyword.equalsIgnoreCase("đã duyệt") || cleanKeyword.equalsIgnoreCase("duyệt")) {
            searchStatus = "COMPLETED";
        } else if (cleanKeyword.equalsIgnoreCase("đã hủy") || cleanKeyword.equalsIgnoreCase("hủy")) {
            searchStatus = "CANCELLED";
        } else if (cleanKeyword.equalsIgnoreCase("chờ") || cleanKeyword.equalsIgnoreCase("chờ duyệt")) {
            searchStatus = "PENDING";
        }

        // Gọi truy vấn tìm kiếm nâng cao (Custom Query) từ Repository
        if (isAdmin) {
            // ADMIN
            return stockOutRepository.searchMultiFields(cleanKeyword, searchStatus, pageable);
        } else {
            // STAFF
            return stockOutRepository.searchMultiFieldsByUserId(currentUserId, cleanKeyword, searchStatus, pageable);
        }
    }

    // Kéo danh sách chờ duyệt cho ngăn kéo Offcanvas
    public List<StockOut> getPendingStockOuts() {
        return stockOutRepository.findByStatus("PENDING");
    }
}