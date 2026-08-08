package com.synq.backend.domain.reference.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.synq.backend.support.SampleDocuments;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class FileTextExtractorTest {

	private static final String KOREAN_BODY =
			"SynQ 회의 어시스턴트 설계 문서입니다. 기록보다 이해, 요약보다 협업을 지향합니다. "
					+ "이 문단은 최소 길이 오십자 조건을 넉넉히 넘기기 위한 본문입니다.";
	private static final String ASCII_BODY =
			"SynQ meeting assistant design document. "
					+ "This paragraph is long enough to clear the fifty character minimum.";

	private final FileTextExtractor extractor =
			new FileTextExtractor(new FileExtractionProperties(50, 200_000));

	@Test
	void PDF_에서_본문을_뽑는다() throws Exception {
		byte[] bytes = SampleDocuments.pdf(ASCII_BODY);

		String text = extractor.extract(new ByteArrayInputStream(bytes), "design.pdf");

		assertThat(text).contains("SynQ meeting assistant design document");
	}

	@Test
	void DOCX_에서_본문을_뽑는다() throws Exception {
		byte[] bytes = SampleDocuments.docx(KOREAN_BODY);

		String text = extractor.extract(new ByteArrayInputStream(bytes), "design.docx");

		assertThat(text).contains("기록보다 이해, 요약보다 협업");
	}

	@Test
	void PPTX_에서_본문을_뽑는다() throws Exception {
		byte[] bytes = SampleDocuments.pptx(KOREAN_BODY);

		String text = extractor.extract(new ByteArrayInputStream(bytes), "deck.pptx");

		assertThat(text).contains("기록보다 이해, 요약보다 협업");
	}

	@Test
	void UTF8_TXT_에서_본문을_뽑는다() {
		byte[] bytes = SampleDocuments.txt(KOREAN_BODY, StandardCharsets.UTF_8);

		String text = extractor.extract(new ByteArrayInputStream(bytes), "notes.txt");

		assertThat(text).contains("기록보다 이해, 요약보다 협업");
	}

	@Test
	void EUC_KR_TXT_도_깨지지_않는다() {
		// Tika 가 인코딩을 감지한다. 직접 new String(bytes, UTF_8) 로 읽으면 여기서 깨진다.
		byte[] bytes = SampleDocuments.txt(KOREAN_BODY, Charset.forName("EUC-KR"));

		String text = extractor.extract(new ByteArrayInputStream(bytes), "legacy.txt");

		assertThat(text).contains("기록보다 이해");
	}

	@Test
	void 확장자를_위장해도_실제_형식으로_파싱한다() throws Exception {
		// 파일명은 타입 감지의 힌트일 뿐이고 매직바이트가 이긴다.
		// 위장 파일을 따로 걸러내지 않는 근거다.
		byte[] bytes = SampleDocuments.docx(KOREAN_BODY);

		String text = extractor.extract(new ByteArrayInputStream(bytes), "disguised.pdf");

		assertThat(text).contains("기록보다 이해, 요약보다 협업");
	}

	@Test
	void 암호가_걸린_PDF_는_ENCRYPTED_다() throws Exception {
		byte[] bytes = SampleDocuments.encryptedPdf(ASCII_BODY);

		assertThatThrownBy(() -> extractor.extract(new ByteArrayInputStream(bytes), "locked.pdf"))
				.isInstanceOfSatisfying(FileTextExtractionException.class, exception ->
						assertThat(exception.getReason())
								.isEqualTo(FileExtractionFailureReason.ENCRYPTED));
	}

	@Test
	void 손상된_파일은_CORRUPTED_다() {
		// PDF 헤더만 있고 본문이 깨진 바이트. 파서가 진입은 하지만 끝까지 읽지 못한다.
		byte[] bytes = "%PDF-1.4\n<<<<<<< broken garbage >>>>>>>".getBytes(StandardCharsets.UTF_8);

		assertThatThrownBy(() -> extractor.extract(new ByteArrayInputStream(bytes), "broken.pdf"))
				.isInstanceOfSatisfying(FileTextExtractionException.class, exception ->
						assertThat(exception.getReason())
								.isEqualTo(FileExtractionFailureReason.CORRUPTED));
	}

	@Test
	void 텍스트가_50자_미만이면_NO_TEXT_LAYER_다() {
		// 텍스트 레이어가 없는 스캔 PDF 와 같은 상황이다.
		byte[] bytes = SampleDocuments.txt("짧은 메모", StandardCharsets.UTF_8);

		assertThatThrownBy(() -> extractor.extract(new ByteArrayInputStream(bytes), "short.txt"))
				.isInstanceOfSatisfying(FileTextExtractionException.class, exception ->
						assertThat(exception.getReason())
								.isEqualTo(FileExtractionFailureReason.NO_TEXT_LAYER));
	}

	@Test
	void 정확히_50자면_통과한다() {
		String exactly50 = "가".repeat(50);
		byte[] bytes = SampleDocuments.txt(exactly50, StandardCharsets.UTF_8);

		assertThat(extractor.extract(new ByteArrayInputStream(bytes), "boundary.txt"))
				.hasSize(50);
	}

	@Test
	void 상한을_넘으면_실패가_아니라_앞부분만_돌려준다() {
		FileTextExtractor limited =
				new FileTextExtractor(new FileExtractionProperties(50, 1_000));
		byte[] bytes = SampleDocuments.txt("가".repeat(5_000), StandardCharsets.UTF_8);

		String text = limited.extract(new ByteArrayInputStream(bytes), "long.txt");

		assertThat(text).hasSizeLessThanOrEqualTo(1_000).isNotEmpty();
	}
}
