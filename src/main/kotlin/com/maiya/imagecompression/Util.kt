package com.maiya.imagecompression

import com.intellij.openapi.vfs.VirtualFile
import java.awt.Image
import java.awt.image.BufferedImage
import kotlin.math.min

const val TINIFY_KEY = "XYghx9jynBD0pvPf4bSrTq97FKJXLgFD"
const val MAX_WIDTH = 300
const val MAX_HEIGHT = 300
const val FLOAT_FROMAT = "%.2f"


val VirtualFile?.isImg
    get() = this?.let {
        extension.equals("jpeg", ignoreCase = true)
                || extension.equals("png", ignoreCase = true)
                || extension.equals("jpg", ignoreCase = true)
    } ?: false

/**
 * 保持宽高比缩放图片,确保图片不超过最大宽高
 */
fun getScaledImage(srcImg: BufferedImage, maxWidth: Int, maxHeight: Int): Image? {
    // 计算新的尺寸以保持宽高比
    val widthRatio = maxWidth.toDouble() / srcImg.width
    val heightRatio = maxHeight.toDouble() / srcImg.height
    val ratio = min(widthRatio, heightRatio)

    val newWidth = (srcImg.width * ratio).toInt()
    val newHeight = (srcImg.height * ratio).toInt()
    // 返回调整大小后的图片
    return srcImg.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH)
}