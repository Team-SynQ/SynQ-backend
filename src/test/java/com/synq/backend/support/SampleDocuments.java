package com.synq.backend.support;

import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

/**
 * 추출기 테스트용 샘플 문서를 코드로 만든다.
 */
public final class SampleDocuments {

	private SampleDocuments() {
	}

	/**
	 * PDF 는 Standard14 폰트(Helvetica)를 쓰므로 <b>ASCII 만</b> 넣을 수 있다.
	 * 한글을 넣으면 showText 가 IllegalArgumentException 을 던진다.
	 */
	public static byte[] pdf(String asciiText) throws IOException {
		try (PDDocument document = new PDDocument();
			 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			writePage(document, asciiText);
			document.save(out);
			return out.toByteArray();
		}
	}

	/** 사용자 암호가 걸린 PDF. Tika 가 EncryptedDocumentException 을 던진다. */
	public static byte[] encryptedPdf(String asciiText) throws IOException {
		try (PDDocument document = new PDDocument();
			 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			writePage(document, asciiText);
			StandardProtectionPolicy policy =
					new StandardProtectionPolicy("owner-password", "user-password", new AccessPermission());
			policy.setEncryptionKeyLength(128);
			document.protect(policy);
			document.save(out);
			return out.toByteArray();
		}
	}

	public static byte[] docx(String text) throws IOException {
		try (XWPFDocument document = new XWPFDocument();
			 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			document.createParagraph().createRun().setText(text);
			document.write(out);
			return out.toByteArray();
		}
	}

	public static byte[] pptx(String text) throws IOException {
		try (XMLSlideShow slideShow = new XMLSlideShow();
			 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			XSLFSlide slide = slideShow.createSlide();
			XSLFTextBox textBox = slide.createTextBox();
			textBox.setAnchor(new Rectangle(50, 50, 400, 200));
			textBox.setText(text);
			slideShow.write(out);
			return out.toByteArray();
		}
	}

	public static byte[] txt(String text, Charset charset) {
		return text.getBytes(charset);
	}

	private static void writePage(PDDocument document, String asciiText) throws IOException {
		PDPage page = new PDPage();
		document.addPage(page);
		try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
			stream.beginText();
			stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
			stream.newLineAtOffset(50, 700);
			stream.showText(asciiText);
			stream.endText();
		}
	}
}
