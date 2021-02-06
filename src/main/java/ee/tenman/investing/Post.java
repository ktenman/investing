package ee.tenman.investing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Post {
    private Long userId;
    private Long id;
    private String title;
    private String body;
}
