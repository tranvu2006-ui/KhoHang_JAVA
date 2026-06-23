package fit.tdc.edu.FEJAVA2.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

@Service
public class ExportService {

    @Autowired
    private RestTemplate restTemplate;

    // Địa chỉ API xuất báo cáo Excel của Backend (cổng 8080)
    private final String BACKEND_EXPORT_URL = "http://localhost:8080/api/export";

    /**
     * 🌟 GỌI API BACKEND: Xuất file Excel báo cáo danh sách Nhập kho
     */
    public ResponseEntity<byte[]> exportStockIn(String token, LocalDate startDate, LocalDate endDate, String status) {
        HttpHeaders headers = new HttpHeaders();
        // 1. Đính kèm JWT Token để Backend xác thực quyền hạn
        headers.set("Authorization", "Bearer " + token);
        // 2. Định nghĩa định dạng nhận về là file Excel .xlsx
        headers.set("Accept", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        // 3. Ghép đường dẫn API kèm theo các tham số lọc khoảng ngày và trạng thái
        String url = BACKEND_EXPORT_URL + "/stock-in?startDate=" + startDate + "&endDate=" + endDate + "&status=" + status;

        // 4. Thực hiện cuộc gọi GET API dạng nhị phân byte[] (để tải tệp tin về)
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
    }

    /**
     * 🌟 GỌI API BACKEND: Xuất file Excel báo cáo danh sách Xuất kho
     */
    public ResponseEntity<byte[]> exportStockOut(String token, LocalDate startDate, LocalDate endDate, String status) {
        HttpHeaders headers = new HttpHeaders();
        // 1. Đính kèm JWT Token để Backend xác thực quyền hạn
        headers.set("Authorization", "Bearer " + token);
        // 2. Định nghĩa định dạng nhận về là file Excel .xlsx
        headers.set("Accept", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        // 3. Ghép đường dẫn API kèm theo các tham số lọc khoảng ngày và trạng thái
        String url = BACKEND_EXPORT_URL + "/stock-out?startDate=" + startDate + "&endDate=" + endDate + "&status=" + status;

        // 4. Thực hiện cuộc gọi GET API dạng nhị phân byte[] (để tải tệp tin về)
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
    }

    /**
     * 🌟 GỌI API BACKEND: Xuất file Excel danh sách Sản phẩm
     */
    public ResponseEntity<byte[]> exportProducts(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return restTemplate.exchange(BACKEND_EXPORT_URL + "/products", HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
    }

    /**
     * 🌟 GỌI API BACKEND: Xuất file Excel danh sách Danh mục sản phẩm
     */
    public ResponseEntity<byte[]> exportCategories(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return restTemplate.exchange(BACKEND_EXPORT_URL + "/categories", HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
    }

    /**
     * 🌟 GỌI API BACKEND: Xuất file Excel danh sách Khách hàng
     */
    public ResponseEntity<byte[]> exportCustomers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return restTemplate.exchange(BACKEND_EXPORT_URL + "/customers", HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
    }

    /**
     * 🌟 GỌI API BACKEND: Xuất file Excel danh sách Nhà cung cấp
     */
    public ResponseEntity<byte[]> exportSuppliers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return restTemplate.exchange(BACKEND_EXPORT_URL + "/suppliers", HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
    }

    /**
     * 🌟 GỌI API BACKEND: Xuất file Excel danh sách Nhân viên & Tài khoản
     */
    public ResponseEntity<byte[]> exportUsers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return restTemplate.exchange(BACKEND_EXPORT_URL + "/users", HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
    }
}
