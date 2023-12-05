package com.maiya.imagecompression

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.tinify.Tinify
import java.awt.Component
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.swing.*


class ImgCompressAction : AnAction() {
    init {
        Tinify.setKey(TINIFY_KEY)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        // 显示进度条并压缩图片
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "图片压缩中", false) {
            private lateinit var compressedImageBytes: ByteArray

            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                // 实际的压缩逻辑
                try {
                    val source = Tinify.fromBuffer(file.contentsToByteArray())
                    val result = source.toBuffer()
                    compressedImageBytes = result
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    Messages.showMessageDialog(project, ex.message, "压缩出错", Messages.getErrorIcon())
                    return
                }
            }


            override fun onSuccess() {
                // 压缩成功，展示对比弹窗
                val dialog = CompareImagesDialog(project, file, compressedImageBytes)
                dialog.show()
                if (dialog.isOK) {
                    WriteCommandAction.runWriteCommandAction(project) {
                        file.setBinaryContent(compressedImageBytes)
                    }
                    // 刷新文件编辑器
                    FileEditorManager.getInstance(project).openFile(file, true)
                    VirtualFileManager.getInstance().refreshWithoutFileWatcher(false)
                }
            }
        })
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.getData(PlatformDataKeys.VIRTUAL_FILE).isImg
    }
}


class CompareImagesDialog(
    project: Project?,
    private val originalFile: VirtualFile,
    private val compressedImageBytes: ByteArray
) : DialogWrapper(project) {
    private val originalImageIcon: ImageIcon = ImageIcon(getScaledImage(ImageIO.read(originalFile.inputStream), MAX_WIDTH, MAX_HEIGHT))
    private val compressedImageIcon: ImageIcon

    init {
        val compressedImage = ImageIO.read(ByteArrayInputStream(compressedImageBytes))
        compressedImageIcon = ImageIcon(getScaledImage(compressedImage, MAX_WIDTH, MAX_HEIGHT))
        title = "确认使用压缩后图片吗"
        setOKButtonText("替换")
        setCancelButtonText("取消")
        init()
    }

    override fun createCenterPanel(): JComponent {

        val mainPanel = JPanel().apply {
            layout = BoxLayout(this,BoxLayout.Y_AXIS)
        }
        val textPanel = JPanel().apply {
            layout = BoxLayout(this,BoxLayout.Y_AXIS)
        }

        val originalKB = originalFile.length / 1024.toFloat()
        val compressedKB = compressedImageBytes.size / 1024.toFloat()

        textPanel.add(JLabel("原图大小: ${FLOAT_FROMAT.format(originalKB)} KB 压缩后大小: ${FLOAT_FROMAT.format(compressedKB)} KB",SwingConstants.RIGHT))
        textPanel.add(JLabel("本次压缩减少: ${FLOAT_FROMAT.format(originalKB - compressedKB)} KB 压缩率: ${((originalKB - compressedKB) * 100 / originalKB).toInt()}% ",SwingConstants.LEFT))

//         原图部分的面板，使用垂直布局
        val imgPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)

            add(JLabel(originalImageIcon).apply {
                alignmentX = Component.CENTER_ALIGNMENT
            })
            add(Box.createHorizontalStrut(10)) // 添加一个水平间隔
            add(JLabel(compressedImageIcon).apply {
                alignmentX = Component.CENTER_ALIGNMENT
            })
        }

        // 将两个部分面板添加到主面板
        mainPanel.add(textPanel)
        mainPanel.add(imgPanel)
        return mainPanel
    }


}

