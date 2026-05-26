package comic.platform.backend.module.comic;

import comic.platform.backend.module.comic.dto.TocResult;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

public interface ComicService {

    public List<Map<String, String>> search(@RequestParam("keyword") String keyword);

    public TocResult getToc(@RequestParam("url") String detailUrl, Integer sourceId);

    public List<String> getContent(@RequestParam("url") String chapterUrl,Integer sourceId);
}
