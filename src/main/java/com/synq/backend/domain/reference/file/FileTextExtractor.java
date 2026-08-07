package com.synq.backend.domain.reference.file;

import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.EncryptedDocumentException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

/**
 * 참고자료 파일에서 본문 텍스트를 뽑는다.
 *
 * <p>AutoDetectParser 는 매직바이트로 형식을 판정하므로 확장자별 분기가 필요 없다.
 * 대신 확장자를 위장한 파일도 실제 형식으로 파싱되는데, 의도한 동작이다.
 *
 * <p>등록 요청 스레드에서 동기로 돌아간다. 링크 추출과 달리 네트워크가 없어 요청 안에서 끝난다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileTextExtractor {

	private final FileExtractionProperties properties;

	/**
	 * @param inputStream 호출자가 닫는다. 여기서 닫지 않는다
	 * @throws FileTextExtractionException 암호·손상·텍스트 없음
	 */
	public String extract(InputStream inputStream, String fileName) {
		BodyContentHandler handler = new BodyContentHandler(properties.maxTextChars());
		// 파일명을 힌트로 준다. 없으면 매직바이트만으로 판정하는데, EUC-KR 텍스트처럼 시그니처가
		// 없는 입력이 application/octet-stream 으로 떨어져 EmptyParser 가 걸린다.
		// 매직바이트가 형식을 알아보는 경우에는 그쪽이 이기므로 확장자 위장은 여전히 통하지 않는다.
		Metadata metadata = new Metadata();
		metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
		try {
			new AutoDetectParser().parse(inputStream, handler, metadata, new ParseContext());
		} catch (EncryptedDocumentException e) {
			// TikaException 의 하위 타입이라 아래 catch 보다 먼저 잡아야 한다.
			throw new FileTextExtractionException(FileExtractionFailureReason.ENCRYPTED, e);
		} catch (TikaException | SAXException | IOException | RuntimeException e) {
			// 상한 도달은 실패가 아니다. 거기까지 담긴 텍스트를 그대로 쓴다.
			// Tika 가 예외를 감싸는 경우가 있어 정적 헬퍼로 판정한다.
			if (!WriteLimitReachedException.isWriteLimitReached(e)) {
				throw new FileTextExtractionException(FileExtractionFailureReason.CORRUPTED, e);
			}
			log.warn("추출 상한({}자)에 도달해 앞부분만 인덱싱한다. fileName={}",
					properties.maxTextChars(), fileName);
		}

		String text = handler.toString().strip();
		if (text.length() < properties.minTextLength()) {
			throw new FileTextExtractionException(FileExtractionFailureReason.NO_TEXT_LAYER, null);
		}
		return text;
	}
}
