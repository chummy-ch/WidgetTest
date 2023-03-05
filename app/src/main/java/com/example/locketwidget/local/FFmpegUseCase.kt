package com.example.locketwidget.local

/*
import android.os.Environment
import android.util.Log
import com.example.locketwidget.core.Result
import com.example.locketwidget.data.timelineAudio
import com.simform.videooperations.*
import java.io.File
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private data class TimelineFrameModel(
    val paths: Paths,
    val duration: String,
)

class FFmpegUseCase {
    companion object {
        private const val TAG = "FFmpegEditor"
        private const val VIDEO_FORMAT = ".mp4"
    }

    private val ffmpeg by lazy { FFmpegQueryExtension() }

    suspend fun addAudio(videoPath: String, name: String, outputPath: String, audioPath: String) = suspendCoroutine<Result<String>> { con ->
        var output = File("$outputPath${File.separator}$name$VIDEO_FORMAT")
        var index = 1
        while (output.exists()) {
            output = File("$outputPath${File.separator}$name$index$VIDEO_FORMAT")
            index++
        }

        val query = ffmpeg.mergeAudioVideo(
            videoPath,
            audioPath,
            output.path
        )
        CallBackOfQuery().callQuery(query, object : FFmpegCallBack {
            override fun statisticsProcess(statistics: Statistics) {
                Log.i(TAG, statistics.videoFrameNumber.toString())
            }

            override fun process(logMessage: LogMessage) {
                Log.i(TAG, logMessage.text)
            }

            override fun success() {
                con.resume(Result.Success(output.path))
                Log.d(TAG, "audio done")

            }

            override fun failed() {
                Log.d(TAG, "fail")
                con.resume(Result.Error(Exception()))
            }
        })
    }

    suspend fun createTimeline(images: List<String>) = suspendCoroutine<Result<String>> { con ->
        val frameTime = (timelineAudio.duration - timelineAudio.firstFrame).toDouble() / (images.size - 1).toDouble()

        val arrayList = ArrayList(
            images.mapIndexed { index, path ->
                TimelineFrameModel(
                    paths = Paths().apply {
                        filePath = path
                        isImageFile = true
                    },
                    duration = if (index == 0) {
                        "${timelineAudio.firstFrame}ms"
                    } else {
                        "${frameTime}ms"
                    }
                )
            }
        )
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "${File.separator}timeline${Date().time}$VIDEO_FORMAT"
        )

        ffmpeg.FRAME_RATE = if (frameTime > 1000) 2 else (1000 / frameTime * 2).toInt()

        val query = ffmpeg.combineImagesToVideo(arrayList, 2000, 2000, file.path)
        CallBackOfQuery().callQuery(query, object : FFmpegCallBack {

            override fun process(logMessage: LogMessage) {
                Log.i(TAG, logMessage.text)
            }

            override fun success() {
                Log.d(TAG, "video done")
                con.resume(Result.Success(file.path))
            }

            override fun failed() {
                Log.d(TAG, "fail")
                con.resume(Result.Error(Exception()))
            }
        })
    }

    private fun FFmpegQueryExtension.combineImagesToVideo(
        frames: ArrayList<TimelineFrameModel>,
        width: Int?,
        height: Int?,
        output: String
    ): Array<String> {
        val inputs: ArrayList<String> = ArrayList()
        for (i in 0 until frames.size) {
            //for input
            if (frames[i].paths.isImageFile) {
                inputs.add("-loop")
                inputs.add("1")
                inputs.add("-framerate")
                inputs.add("$FRAME_RATE")
                inputs.add("-t")
                inputs.add(frames[i].duration)
                inputs.add("-i")
                inputs.add(frames[i].paths.filePath)
            } else {
                inputs.add("-i")
                inputs.add(frames[i].paths.filePath)
            }
        }

        var query: String? = ""
        var queryAudio: String? = ""
        for (i in 0 until frames.size) {
            query = query?.trim()
            query += "[" + i + ":v]scale=${width}x${height},setdar=$width/$height[" + i + "v];"

            queryAudio = queryAudio?.trim()
            queryAudio += if (frames[i].paths.isImageFile) {
                "[" + i + "v][" + frames.size + ":a]"
            } else {
                "[" + i + "v][" + i + ":a]"
            }
        }
        inputs.apply {
            add("-f")
            add("lavfi")
            add("-t")
            add("0.1")
            add("-i")
            add("anullsrc")
            add("-filter_complex")
            add(query + queryAudio + "concat=n=" + frames.size + ":v=1:a=1 [v][a]")
            add("-map")
            add("[v]")
            add("-map")
            add("[a]")
            add("-preset")
            add("ultrafast")
            add(output)
        }
        return inputs.toArray(arrayOfNulls<String>(inputs.size))
    }
}
*/
