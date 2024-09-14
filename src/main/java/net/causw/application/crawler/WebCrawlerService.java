package net.causw.application.crawler;

import lombok.RequiredArgsConstructor;
import net.causw.adapter.persistence.crawled.CrawledNotice;
import net.causw.adapter.persistence.crawled.LatestCrawl;
import net.causw.adapter.persistence.repository.CrawledNoticeRepository;
import net.causw.adapter.persistence.repository.LatestCrawlRepository;
import net.causw.domain.model.enums.CrawlCategory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class WebCrawlerService {
    private static final String LATEST_URL_KEY = "latestUrl";

    private final CrawledNoticeRepository crawledNoticeRepository;
    private final LatestCrawlRepository latestCrawlRepository;


//    @Scheduled(fixedRate = 5000) // 5초마다 실행 (테스트용)
    @Scheduled(cron = "0 0 * * * *") // 매 시각 0분 0초에 실행 (배포용)
    @Transactional
    public void crawlAndSaveCAUSWNoticeSite() throws IOException {
        String baseUrl = "https://cse.cau.ac.kr/sub05/sub0501.php?offset=";
        int pageNum = 1;

        // 최신 URL 가져오기
        String recentNoticeLink = latestCrawlRepository.findByCrawlCategory(CrawlCategory.CAU_SW_NOTICE)
                .map(LatestCrawl::getLatestUrl)
                .orElse(null);

        boolean isNew = true;
        while (isNew) {
            String url = baseUrl + pageNum;
            Document doc = Jsoup.connect(url).get();
            Elements rows = doc.select("table.table-basic tbody tr");

            if (rows.isEmpty()) {
                break; // 더 이상 페이지가 없으면 종료
            }

            List<CrawledNotice> notices = new ArrayList<>();

            for (Element row : rows) {
                String noticeType = row.select("td span.tag").text();
                Element titleElement = row.select("td.aleft a").first();
                if (titleElement == null) {
                    continue;
                }

                String absoluteLink = titleElement.absUrl("href");
                System.out.println("방문한 페이지 절대 경로 : " + absoluteLink);
                System.out.println("DB에 저장되어 있는 가장 최근 공지 URL " + recentNoticeLink);
                if (absoluteLink.equals(recentNoticeLink)) {   // 최신 url과 비교하여 동일한 경우 for문 탈출
                    isNew = false;
                    break;
                }
                // 상세 페이지로 이동하여 내용 크롤링
                Document detailDoc = Jsoup.connect(absoluteLink).get();
                String title = detailDoc.select("div.header > h3").text();  // 제목 추출
                String announceDate = detailDoc.select("div.header > div > span").get(1).text();    // 작성일 추출
                String author = detailDoc.select("div.header > div > span").get(3).text();  // 작성자 추출
                String content = detailDoc.select("div.fr-view").outerHtml();   // 본문 내용 추출

                // CrawledNotice 객체 생성
                CrawledNotice notice = CrawledNotice.of(
                        noticeType,
                        title,
                        content,
                        absoluteLink,
                        author,
                        announceDate
                );
                notices.add(notice);

            }
            // DB save
            if (!notices.isEmpty()) {
                // 새로운 공지사항이 하나라도 있으면 저장
                crawledNoticeRepository.saveAll(notices);

                // 첫 페이지의 첫 번째 공지사항을 저장
                if (pageNum == 1) {
                    if (recentNoticeLink == null) {
                        // 처음 크롤링하는 경우엔 url save
                        latestCrawlRepository.save(LatestCrawl.of(notices.get(0).getLink(), CrawlCategory.CAU_SW_NOTICE));
                    } else {
                        // 최신 공지 URL 업데이트
                        latestCrawlRepository.updateLatestUrlByCategory(notices.get(0).getLink(), CrawlCategory.CAU_SW_NOTICE);
                    }
                }
            } else {
                // 공지가 하나도 없으면 종료
                isNew = false;
            }
            // 다음 페이지로 이동
            pageNum++;
        }
    }
}
