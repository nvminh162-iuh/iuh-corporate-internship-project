package com.hs.user.filter;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserContextHolder {

    String email;
    String userId;

    /**
     * Vùng lưu tạm theo thread hiện tại.
     * Trong servlet request, mỗi thread giữ một bản user context riêng.
     */
    static final ThreadLocal<UserContextHolder> HOLDER = new ThreadLocal<>();

    /**
     * Đặt user context cho request đang được xử lý.
     * Method này được gọi sau khi filter đọc xong các header X-User-*.
     */
    public static void set(UserContextHolder context) {
        HOLDER.set(context);
    }

    /**
     * Trả về user context gắn với thread hiện tại.
     * Nếu request không có thông tin user thì kết quả có thể là null.
     */
    public static UserContextHolder get() {
        return HOLDER.get();
    }

    /**
     * Dọn user context khỏi thread sau khi request kết thúc.
     * Bước này giúp tránh rò rỉ dữ liệu sang request khác khi server tái sử dụng thread.
     */
    public static void clear() {
        HOLDER.remove();
    }

}
