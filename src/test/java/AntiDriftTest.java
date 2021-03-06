import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import spim.algorithm.AntiDrift;
import spim.controller.AntiDriftController;
import spim.algorithm.DefaultAntiDrift;
import utils.ImageGenerator;

import java.awt.*;
import java.io.File;

import static org.junit.Assume.assumeTrue;

/**
 * The type Anti drift handler test.
 */
public class AntiDriftTest
{
	private ImagePlus impFirst;
	private ImagePlus impSecond;
	private ImagePlus impThird;


	/**
	 * Calculates the Gauss function (AKA kernel) of the given parameters.
	 *
	 * @param size the size of the kernel
	 * @param center the center of the Gaussian function
	 * @param sigma the standard deviation of the Gaussian function
	 * @return the kernel
	 */
	private static float[] gauss(final int size, final float center, final float sigma) {
		final float[] result = new float[size];
		for (int i = 0; i < size; i++) {
			final float x = (i - center) / sigma;
			result[i] = (float) Math.exp(-x * x / 2);
		}
		return result;
	}



	/**
	 * setup the images
	 */
	@Before
	public void setup()
	{
		// impFirst = generateBlob(128, 128, 128, 64, 32, 48, 50 / 8.0f , 30 / 4.0f, 40 / 4.0f);
		// impSecond = generateBlob(128, 128, 128, 64 + 16, 32 + 24, 48 + 32, 40 /8.0f, 20 / 4.0f, 30 / 4.0f);
		// The below parameters are simplified version with above ratio
		impFirst = ImageGenerator.generateFloatBlob(128, 128, 128, 64, 32, 48, 12.5f , 7.5f, 10.0f);
		impSecond = ImageGenerator.generateFloatBlob(128, 128, 128, 64 + 16, 32 + 24, 48 + 32, 5.0f, 5.0f, 7.5f);
		impThird = ImageGenerator.generateFloatBlob(128, 128, 128, 64 + 8, 32 + 32, 48 + 24, 4.0f, 4.0f, 6.5f);
	}

	/**
	 * Test anti drift handler.
	 */
	@Test
	public void testAntiDrift()
	{
		final DefaultAntiDrift proj = new DefaultAntiDrift();
		proj.startNewStack();

		final ImageStack stackFirst = impFirst.getImageStack();
		for(int k = 1; k <= stackFirst.getSize(); k++)
		{
			proj.addXYSlice( stackFirst.getProcessor( k ) );
		}
		proj.finishStack( );

		proj.startNewStack();
		final ImageStack stackSecond = impSecond.getImageStack();
		for(int k = 1; k <= stackSecond.getSize(); k++)
		{
			proj.addXYSlice( stackSecond.getProcessor( k ) );
		}

		final Vector3D correction = proj.finishStack( );
		final double DELTA = 1e-5;

		Assert.assertEquals(16, correction.getX(), DELTA);
		Assert.assertEquals(24, correction.getY(), DELTA);
		Assert.assertEquals(32, correction.getZ(), DELTA);
	}

	/**
	 * GUI involved test which is run in main function
	 */
	public void testAntiDriftController()
	{
		assumeTrue(!GraphicsEnvironment.isHeadless());

		final AntiDriftController ct = new AntiDriftController( new File("/Users/moon/temp/"), 2, 3, 4, 0, 1, 0.5);

		ct.setCallback(new AntiDrift.Callback() {
			public void applyOffset(Vector3D offs) {
				offs = new Vector3D(offs.getX()*-1, offs.getY()*-1, -offs.getZ());
				System.out.println(String.format("Offset: %s", offs.toString()));
			}
		});

		ct.startNewStack();

		final ImageStack stackFirst = impFirst.getImageStack();
		for(int k = 1; k <= stackFirst.getSize(); k++)
		{
			ct.addXYSlice( stackFirst.getProcessor( k ) );
		}
		ct.finishStack();

		ct.startNewStack();
		final ImageStack stackSecond = impSecond.getImageStack();
		for(int k = 1; k <= stackSecond.getSize(); k++)
		{
			ct.addXYSlice( stackSecond.getProcessor( k ) );
		}
		ct.finishStack();

//		try
//		{
//			Thread.sleep( 10000 );
//		} catch (InterruptedException e)
//		{
//			e.printStackTrace();
//		}
//
//		ct.startNewStack();
//		final ImageStack stackThird = impThird.getImageStack();
//		for(int k = 1; k <= stackThird.getSize(); k++)
//		{
//			ct.addXYSlice( stackThird.getProcessor( k ) );
//		}
//		ct.finishStack();
	}

	Vector3D updatedOffset = new Vector3D( 0, 0, 0 );

	public void testAntiDriftWithData()
	{
		final AntiDriftController ct = new AntiDriftController( new File("/Users/moon/temp/"), 2, 3, 4, 0, 1, 3);

		ct.setCallback(new AntiDrift.Callback() {
			public void applyOffset(Vector3D offs) {
				offs = updatedOffset = new Vector3D(offs.getX()*1, offs.getY()*1, offs.getZ());
				System.out.println(String.format("Offset: %s", offs.toString()));
			}
		});

		for(int i = 0; i < 5; i++)
		{
			String filename = "spim_TL0" + (i + 1) + "_Angle0.ome.tiff";
			ImagePlus ip = IJ.openImage("/Users/moon/temp/normal/" + filename);
			ct.startNewStack();

			final ImageStack stack = ip.getImageStack();
			for(int k = 1; k <= stack.getSize(); k++)
			{
				FloatProcessor fp = (FloatProcessor) stack.getProcessor( k ).convertToFloat();

				fp.translate( updatedOffset.getX(), updatedOffset.getY() );

				ct.addXYSlice( fp );
			}
			ct.finishStack();

			ip.close();

			try
			{
				Thread.sleep( 10000 );
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] argv)
	{
		AntiDriftTest test = new AntiDriftTest();
		test.testAntiDriftWithData();
//		test.setup();
//		test.testAntiDriftController();
	}
}
