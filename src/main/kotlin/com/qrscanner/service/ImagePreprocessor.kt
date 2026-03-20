package com.qrscanner.service

import org.bytedeco.javacv.Java2DFrameConverter
import org.bytedeco.javacv.OpenCVFrameConverter
import org.bytedeco.opencv.global.opencv_core.*
import org.bytedeco.opencv.global.opencv_imgproc.*
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.Size
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage

class ImagePreprocessor {

    private val logger = LoggerFactory.getLogger(ImagePreprocessor::class.java)

    enum class Strategy {
        GRAYSCALE,
        GRAYSCALE_CLAHE,
        GRAYSCALE_BLUR_THRESHOLD,
        GRAYSCALE_CLAHE_BILATERAL_THRESHOLD
    }

    fun preprocess(image: BufferedImage, strategy: Strategy): BufferedImage {
        // Create converters per-call for thread safety
        val java2dConverter = Java2DFrameConverter()
        val opencvConverter = OpenCVFrameConverter.ToMat()

        val frame = java2dConverter.convert(image)
        val mat = opencvConverter.convert(frame)

        val result = when (strategy) {
            Strategy.GRAYSCALE -> applyGrayscale(mat)
            Strategy.GRAYSCALE_CLAHE -> applyGrayscaleClahe(mat)
            Strategy.GRAYSCALE_BLUR_THRESHOLD -> applyGrayscaleBlurThreshold(mat)
            Strategy.GRAYSCALE_CLAHE_BILATERAL_THRESHOLD -> applyGrayscaleClaheBilateralThreshold(mat)
        }

        val resultFrame = opencvConverter.convert(result)
        val resultImage = java2dConverter.convert(resultFrame)

        // Release native memory
        mat.release()
        result.release()

        logger.debug("Applied preprocessing strategy: {}", strategy)
        return resultImage
    }

    fun isAvailable(): Boolean {
        return try {
            val testMat = Mat()
            testMat.release()
            true
        } catch (e: Exception) {
            logger.warn("OpenCV is not available: {}", e.message)
            false
        }
    }

    private fun applyGrayscale(mat: Mat): Mat {
        val gray = Mat()
        if (mat.channels() > 1) {
            cvtColor(mat, gray, COLOR_BGR2GRAY)
        } else {
            mat.copyTo(gray)
        }
        return gray
    }

    private fun applyGrayscaleClahe(mat: Mat): Mat {
        val gray = applyGrayscale(mat)
        val clahe = createCLAHE(2.0, Size(8, 8))
        val result = Mat()
        clahe.apply(gray, result)
        gray.release()
        return result
    }

    private fun applyGrayscaleBlurThreshold(mat: Mat): Mat {
        val gray = applyGrayscale(mat)
        val blurred = Mat()
        GaussianBlur(gray, blurred, Size(5, 5), 0.0)
        val result = Mat()
        adaptiveThreshold(blurred, result, 255.0, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 11, 2.0)
        gray.release()
        blurred.release()
        return result
    }

    private fun applyGrayscaleClaheBilateralThreshold(mat: Mat): Mat {
        val gray = applyGrayscale(mat)
        val clahe = createCLAHE(3.0, Size(8, 8))
        val claheResult = Mat()
        clahe.apply(gray, claheResult)
        val bilateral = Mat()
        bilateralFilter(claheResult, bilateral, 9, 75.0, 75.0)
        val result = Mat()
        adaptiveThreshold(bilateral, result, 255.0, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 11, 2.0)
        gray.release()
        claheResult.release()
        bilateral.release()
        return result
    }
}
