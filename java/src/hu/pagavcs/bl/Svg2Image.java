package hu.pagavcs.bl;

import java.awt.Paint;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.image.ImageTranscoder;

public class Svg2Image {

	protected TranscoderInput  input;
	protected TranscodingHints hints = new TranscodingHints();
	protected BufferedImage    img;

	public Svg2Image(URL url) {
		this.input = new TranscoderInput(url.toString());
	}

	public Svg2Image(InputStream istream) {
		this.input = new TranscoderInput(istream);
	}

	public Svg2Image(Reader reader) {
		this.input = new TranscoderInput(reader);
	}

	public void setImageWidth(float width) {
		hints.put(ImageTranscoder.KEY_WIDTH, width);
	}

	public void setImageHeight(float height) {
		hints.put(ImageTranscoder.KEY_HEIGHT, height);
	}

	public void setBackgroundColor(Paint p) {
		hints.put(ImageTranscoder.KEY_BACKGROUND_COLOR, p);
	}

	public BufferedImage createImage() throws TranscoderException {
		Rasterizer r = new Rasterizer();
		r.setTranscodingHints(hints);
		r.transcode(input, null);
		return img;
	}

	protected class Rasterizer extends ImageTranscoder {

		public BufferedImage createImage(int w, int h) {
			return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		}

		public void writeImage(BufferedImage img, TranscoderOutput output) throws TranscoderException {
			Svg2Image.this.img = img;
		}
	}
}
