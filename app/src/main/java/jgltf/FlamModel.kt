package jgltf

import android.os.Environment
import android.util.Log
import jgltf.model.GltfModel
import jgltf.model.impl.*
import jgltf.model.impl.DefaultAnimationModel.DefaultChannel
import jgltf.model.io.GltfAssetReader
import jgltf.model.io.GltfModelWriter
import jgltf.model.io.v2.GltfAssetV2
import jgltf.model.io.v2.GltfAssetWriterV2
import jgltf.model.io.v2.GltfAssetsV2
import jgltf.model.io.v2.RawBinaryGltfDataReaderV2
import java.io.File
import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import java.util.*
import kotlin.io.path.toPath

val ANIMATION_URLS = listOf(
    "https://atul-test-m.s3.ap-south-1.amazonaws.com/avtar/bb.glb",
    "https://atul-test-m.s3.ap-south-1.amazonaws.com/avtar/0.glb",
    "https://atul-test-m.s3.ap-south-1.amazonaws.com/avtar/breakdance.glb",
    "https://atul-test-m.s3.ap-south-1.amazonaws.com/avtar/Dancing.fbx.glb",
    "https://atul-test-m.s3.ap-south-1.amazonaws.com/avtar/Hip+Hop+Dancing+(1).fbx.glb",
    "https://atul-test-m.s3.ap-south-1.amazonaws.com/avtar/Jumping+Down+(1).fbx.glb",
    "https://atul-test-m.s3.ap-south-1.amazonaws.com/avtar/Standing+2H+Magic+Attack+01.fbx.glb",
    "https://8thwall.8thwall.app/rpm-aframe/assets/animated-m-9d620bk.glb",
    "https://atul-test-m.s3.ap-south-1.amazonaws.com/Kiss.glb"
)


class FlamModel {
    /**
     * @param newAnimModel
     * @param newAvatarModel
     * Adds animation to the avatar model and returns a new model with animations
     * @return A new DefaultGltfModel with animation
     */
    private fun addAnimation(
        newAnimModel: DefaultGltfModel,
        newAvatarModel: DefaultGltfModel,
    ): DefaultGltfModel {
        val animModels = newAnimModel.animationModels
        val skin = newAvatarModel.skinModels[0]

        for (animationModel in animModels) {
            val defaultAnimationModel = DefaultAnimationModel()
            val channels = animationModel.channels
            for (channel in channels) {
                //animation Node
                val currentTargetNodeName = channel.nodeModel.name
                // Model Node
                val targetNode = skin.joints.find { nodeModel ->
                    nodeModel.name.removePrefix("mixamorig:")
                        .removePrefix("mixamorig_") == currentTargetNodeName.removePrefix(
                        "mixamorig:"
                    ).removePrefix("mixamorig_")
                }
                if (targetNode != null) {
                    val channelWithAnim = DefaultChannel(channel.sampler, targetNode, channel.path)
                    defaultAnimationModel.addChannel(channelWithAnim)
                } else {
                    Log.i("NODE MISSING", currentTargetNodeName)
                }
            }
            newAvatarModel.addAnimationModel(defaultAnimationModel)
        }
        for (accessor in newAnimModel.accessorModels) {
            newAvatarModel.addAccessorModel(accessor as DefaultAccessorModel)
        }

        for (bufferModel in newAnimModel.bufferModels) {
            newAvatarModel.addBufferModel(bufferModel as DefaultBufferModel)
        }
        for (bufferViewModel in newAnimModel.bufferViewModels) {
            newAvatarModel.addBufferViewModel(bufferViewModel as DefaultBufferViewModel)
        }
        return newAvatarModel
    }

    /**
     * @param gltfModel
     * Returns a new in memory byte buffer for the gltfModel
     */
    private fun getByteBuffer(gltfModel: GltfModel): ByteBuffer {
        val gltfAsset = GltfAssetsV2.createBinary(gltfModel)
        val gltfAssetWriter = GltfAssetWriterV2()
        return gltfAssetWriter.writeByte(gltfAsset)
    }

    private fun writeToLocal(model: GltfModel, fileName: String): String {
        val writer = GltfModelWriter()
        val file = File(
            Environment.getExternalStorageDirectory()
                .toString() + "/Download/" + File.separator + fileName
        )
        writer.writeBinary(model, file)
        return file.path
    }

    /**
     * @param modelUrl : URL of the model in string
     * @param animationUrl : OPTIONAL URl of the animation to be appended
     * animationUrl has a default value
     */
    fun getAnimatedModelBuffer(
        modelUrl: String,
        animationUrl: String = ANIMATION_URLS.random()
    ): ByteBuffer? {
        val gltfAssetReader = GltfAssetReader()

        val animGltfAsset = gltfAssetReader.read(URI(animationUrl))
        val modelGltfAsset = gltfAssetReader.read(URI(modelUrl))

        val newAnimModel =
            jgltf.model.v2.GltfModelCreatorV2.create(animGltfAsset as GltfAssetV2?)
        val newAvatarModel =
            jgltf.model.v2.GltfModelCreatorV2.create(modelGltfAsset as GltfAssetV2?)

        val finalModel = addAnimation(
            newAnimModel,
            newAvatarModel,
        )
        return getByteBuffer(finalModel)
    }

    /**
     * @param modelPath: Path of the Avatar file stored in storage
     * @param animationPath: Path of the animation file stored in Storage
     */
    fun getAnimatedModelBuffer(
        modelPath: Path,
        animationPath: Path = URI(ANIMATION_URLS.random()).toPath()
    ): ByteBuffer? {
        try {

            val gltfAssetReader = GltfAssetReader()

            val animGltfAsset = gltfAssetReader.read(modelPath)
            val modelGltfAsset = gltfAssetReader.read(animationPath)

            val newAnimModel =
                jgltf.model.v2.GltfModelCreatorV2.create(animGltfAsset as GltfAssetV2?)
            val newAvatarModel =
                jgltf.model.v2.GltfModelCreatorV2.create(modelGltfAsset as GltfAssetV2?)

            val finalModel = addAnimation(
                newAnimModel,
                newAvatarModel,
            )
            return getByteBuffer(finalModel)
        } catch (e: Exception) {
            Log.i("ERROR", e.message.toString())
        }
        return null
    }

    /**
     * @param modelBuffer: byteBuffer of the model
     * @return ByteBuffer
     */
    fun getAnimatedModelBuffer(
        modelBuffer: ByteBuffer,
        animationUrl: String = ANIMATION_URLS.random()
    ): ByteBuffer {
        try {
            Log.i("animation", "${animationUrl[0]}")
            val gltfAssetReader = GltfAssetReader()

            val modelRawGltfAsset =
                RawBinaryGltfDataReaderV2.readBinaryGltf(modelBuffer.order(ByteOrder.LITTLE_ENDIAN))

            val modelGltfAsset = gltfAssetReader.read(modelRawGltfAsset)
            val animGltfAsset = gltfAssetReader.read(URI(animationUrl))

            val newAnimModel =
                jgltf.model.v2.GltfModelCreatorV2.create(animGltfAsset as GltfAssetV2?)
            val newAvatarModel =
                jgltf.model.v2.GltfModelCreatorV2.create(modelGltfAsset as GltfAssetV2?)

            val finalModel = addAnimation(
                newAnimModel,
                newAvatarModel,
            )
            return getByteBuffer(finalModel)

        } catch (e: Exception) {
            Log.i("ERROR", e.message.toString())

        }
        return modelBuffer
    }

    fun getBufferOfAnimation(
        animationUrl: String = ANIMATION_URLS.random()
    ): ByteBuffer? {
        try {
            Log.i("animation", "${animationUrl[0]}")
            val gltfAssetReader = GltfAssetReader()
            val animGltfAsset = gltfAssetReader.read(URI(animationUrl))

            val newAnimModel =
                jgltf.model.v2.GltfModelCreatorV2.create(animGltfAsset as GltfAssetV2?)

            return getByteBuffer(newAnimModel)

        } catch (e: Exception) {
            Log.i("ERROR", e.toString())

        }
        return null
    }

    /**
     * @param modelBuffer: byteBuffer of the model
     * @param animationBuffer: byteBuffer of the model
     * @return ByteBuffer
     */
    fun getAnimatedModelBuffer(modelBuffer: ByteBuffer, animationBuffer: ByteBuffer): ByteBuffer? {
        try {
            val gltfAssetReader = GltfAssetReader()

            val modelRawGltfAsset =
                RawBinaryGltfDataReaderV2.readBinaryGltf(modelBuffer.order(ByteOrder.LITTLE_ENDIAN))
            val animationRawGltfAsset =
                RawBinaryGltfDataReaderV2.readBinaryGltf(animationBuffer.order(ByteOrder.LITTLE_ENDIAN))

            val modelGltfAsset = gltfAssetReader.read(modelRawGltfAsset)
            val animGltfAsset = gltfAssetReader.read(animationRawGltfAsset)

            val newAnimModel =
                jgltf.model.v2.GltfModelCreatorV2.create(animGltfAsset as GltfAssetV2?)
            val newAvatarModel =
                jgltf.model.v2.GltfModelCreatorV2.create(modelGltfAsset as GltfAssetV2?)
            val finalModel = addAnimation(
                newAnimModel,
                newAvatarModel,
            )
            return getByteBuffer(finalModel)

        } catch (e: Exception) {
            Log.i("ERROR", e.message.toString())

        }
        return modelBuffer
    }


    /**
     * @param modelUrl: Url of the Avatar file
     * @param animationPath: URl of the animation file
     * @return Path of the generated model in storage
     */
    fun getAnimatedModel(modelUrl: String, animationUrl: String = ANIMATION_URLS.random()): String {
        val gltfAssetReader = GltfAssetReader()

        val animGltfAsset = gltfAssetReader.read(URI(animationUrl))
        val modelGltfAsset = gltfAssetReader.read(URI(modelUrl))

        val newAnimModel =
            jgltf.model.v2.GltfModelCreatorV2.create(animGltfAsset as GltfAssetV2?)
        val newAvatarModel =
            jgltf.model.v2.GltfModelCreatorV2.create(modelGltfAsset as GltfAssetV2?)

        val finalModel = addAnimation(
            newAnimModel,
            newAvatarModel,
        )

        return writeToLocal(finalModel, "${UUID.randomUUID()}.glb")
    }

    /**
     * @param modelPath: Path the Avatar file
     * @param animationPath: Path of the animation file
     * @return Path of the generated model in storage
     */
    fun getAnimatedModel(
        modelPath: Path,
        animationPath: Path = URI(ANIMATION_URLS[0]).toPath()
    ): String {
        val gltfAssetReader = GltfAssetReader()

        val animGltfAsset = gltfAssetReader.read(animationPath)
        val modelGltfAsset = gltfAssetReader.read(modelPath)

        val newAnimModel =
            jgltf.model.v2.GltfModelCreatorV2.create(animGltfAsset as GltfAssetV2?)
        val newAvatarModel =
            jgltf.model.v2.GltfModelCreatorV2.create(modelGltfAsset as GltfAssetV2?)

        val finalModel = addAnimation(
            newAnimModel,
            newAvatarModel,
        )

        return writeToLocal(finalModel, "${UUID.randomUUID()}.glb")

    }

    /**
     * @param modelbuffer: ByteBuffer of Avatar file
     * @return Path of the generated model in storage
     */
    fun getAnimatedModel(modelBuffer: ByteBuffer): String? {
        try {
            val gltfAssetReader = GltfAssetReader()

            val modelBufferEL = ByteBuffer.wrap(modelBuffer.array()).order(ByteOrder.LITTLE_ENDIAN)
            val modelRawGltfAsset =
                RawBinaryGltfDataReaderV2.readBinaryGltf(modelBufferEL)
            val modelGltfAsset = gltfAssetReader.read(modelRawGltfAsset)
            val animGltfAsset = gltfAssetReader.read(URI(ANIMATION_URLS[7]))
            val newAnimModel =
                jgltf.model.v2.GltfModelCreatorV2.create(animGltfAsset as GltfAssetV2?)
            val newAvatarModel =
                jgltf.model.v2.GltfModelCreatorV2.create(modelGltfAsset as GltfAssetV2?)
            val finalModel = addAnimation(
                newAnimModel,
                newAvatarModel,
            )

            return writeToLocal(finalModel, "${UUID.randomUUID()}.glb")
        } catch (e: Exception) {
            Log.i("ERROR:", e.message.toString())

        }
        return null
    }

}