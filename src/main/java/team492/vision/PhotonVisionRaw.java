/*
 * Copyright (c) 2024 Titan Robotics Club (http://www.titanrobotics.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package team492.vision;

import java.io.IOException;
import java.util.Optional;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import frclib.vision.FrcPhotonVisionRaw;
import team492.subsystems.LEDIndicator;
import team492.vision.PhotonVision.PipelineType;
import trclib.pathdrive.TrcPose3D;
import trclib.timer.TrcTimer;

/**
 * This class is a thin wrapper extending FrcPhotonVision that provides additional game specific functionalities.
 */
public class PhotonVisionRaw extends FrcPhotonVisionRaw
{
    private final LEDIndicator ledIndicator;
    private final AprilTagFieldLayout aprilTagFieldLayout;
    // private final PhotonPoseEstimator poseEstimator;
    private PipelineType currPipeline;

    /**
     * Constructor: Create an instance of the object.
     *
     * @param tableName specifies the network table name that PhotonVision is broadcasting information over.
     * @param cameraName specifies the camera name.
     * @param ledIndicator specifies the LEDIndicator object, can be null if none provided.
     */
    public PhotonVisionRaw(String tableName, String cameraName, LEDIndicator ledIndicator)
    {
        super(tableName, cameraName);
        this.ledIndicator = ledIndicator;

        double startTime = TrcTimer.getModeElapsedTime();
        try
        {
            aprilTagFieldLayout = AprilTagFieldLayout.loadFromResource(AprilTagFields.k2024Crescendo.m_resourceFile);
            // poseEstimator = new PhotonPoseEstimator(
            //     aprilTagFieldLayout, PoseStrategy.MULTI_TAG_PNP_ON_COPROCESSOR, this,
            //     RobotParams.CAMERA_TRANSFORM3D);
            // poseEstimator.setMultiTagFallbackStrategy(PoseStrategy.LOWEST_AMBIGUITY);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to load AprilTag field layout info.");
        }
        double endTime = TrcTimer.getModeElapsedTime();
        tracer.traceDebug(instanceName, "Loading AprilTag field layout took " + (endTime - startTime) + " sec.");

        setPipeline(PipelineType.APRILTAG);
    }   //PhotonVisionRaw

    /**
     * This method returns the best detected object and set the LED to indicate type detected object type.
     *
     * @return best detected object.
     */
    @Override
    public DetectedObject getDetectedObject()
    {
        DetectedObject detectedObject = super.getDetectedObject();

        if (detectedObject != null && ledIndicator != null)
        {
            ledIndicator.setPhotonDetectedObject(getPipeline(), detectedObject.getObjectPose());
        }

        return detectedObject;
    }   //getDetectedObject

    /**
     * This method returns the 3D field location of the AprilTag with its given ID.
     *
     * @param aprilTagId sepcifies the AprilTag ID to retrieve its field location.
     * @return 3D location of the AprilTag.
     */
    public TrcPose3D getAprilTagPose(int aprilTagId)
    {
        Optional<Pose3d> tagPose = aprilTagFieldLayout.getTagPose(aprilTagId);
        Pose3d pose3d = tagPose.isPresent()? tagPose.get(): null;
        Rotation3d rotation = pose3d != null? pose3d.getRotation(): null;
        return pose3d != null?
                new TrcPose3D(-pose3d.getY(), pose3d.getX(), pose3d.getZ(),
                              -rotation.getZ(), rotation.getY(), rotation.getX()): null;
    }   //getAprilTagPose

    // /**
    //  * This method returns the absolute field location of the camera with the given detected AprilTag object.
    //  *
    //  * @param detectedObj specifies the AprilTag object detected by the camera.
    //  * @return camera's absolute field location.
    //  */
    // public TrcPose2D getRobotFieldPosition(DetectedObject detectedObj)
    // {
    //     TrcPose2D robotPose = null;
    //     int aprilTagId = (Integer) detectedObj.target;
    //     // aprilTagPose is the absolute field position of the AprilTag.
    //     Pose3d aprilTagPose = getAprilTagPose(aprilTagId);

    //     if (aprilTagPose != null)
    //     {
    //         // camPose3d is the absolute field position of the camera.
    //         Pose3d camPose3d = aprilTagPose.transformBy(detectedObj.target.getBestCameraToTarget().inverse());
    //         // robotPose3d is the absolute 3D field position of the robot centroid on the ground.
    //         Pose3d robotPose3d = camPose3d.transformBy(RobotParams.CAMERA_TRANSFORM3D.inverse());
    //         // robotPose is the absolute field position of the robot adjusted to the robot coordinate system.
    //         robotPose = DetectedObject.pose3dToTrcPose2D(robotPose3d);
    //         tracer.traceDebug(
    //             instanceName,
    //             "[" + aprilTagId + "] camPose3d=" + camPose3d +
    //             ", robotPose3d=" + robotPose3d +
    //             ", RobotPose=" + robotPose);
    //     }

    //     return robotPose;
    // }   //getRobotFieldPosition

    // /**
    //  * This method uses the PhotonVision Pose Estimator to get an estimated absolute field position of the robot.
    //  *
    //  * @return absolute robot field position, can be null if not provided.
    //  */
    // public TrcPose2D getEstimatedFieldPosition(TrcPose2D robotPose)
    // {
    //     TrcPose2D estimatedRobotPose = null;

    //     if (poseEstimator != null)
    //     {
    //         if (robotPose != null)
    //         {
    //             poseEstimator.setReferencePose(DetectedObject.trcPose2DToPose3d(robotPose));
    //         }
    //         Optional<EstimatedRobotPose> optionalPose = poseEstimator.update();
    //         if (optionalPose.isPresent())
    //         {
    //             estimatedRobotPose = DetectedObject.pose3dToTrcPose2D(optionalPose.get().estimatedPose);
    //         }
    //     }

    //     return estimatedRobotPose;
    // }   //getEstimatedFieldPosition

    /**
     * This method sets the active pipeline type used in the LimeLight.
     *
     * @param pipelineType specifies the pipeline to activate in the LimeLight.
     */
    public void setPipeline(PipelineType pipelineType)
    {
        if (pipelineType != currPipeline)
        {
            currPipeline = pipelineType;
            super.selectPipeline(pipelineType.pipelineIndex);
        }
    }   //setPipeline

    /**
     * This method returns the active pipeline of the LimeLight.
     *
     * @return active pipeline.
     */
    public PipelineType getPipeline()
    {
        currPipeline = PipelineType.values()[super.getSelectedPipeline()];
        return currPipeline;
    }   //getPipeline

    //
    // Implements FrcPhotonVision abstract methods.
    //

    // /**
    //  * This method returns the ground offset of the detected target.
    //  *
    //  * @return target ground offset.
    //  */
    // @Override
    // public double getTargetHeight(PhotonTrackedTarget target)
    // {
    //     double targetHeight = 0.0;
    //     PipelineType pipelineType = getPipeline();

    //     switch (pipelineType)
    //     {
    //         case APRILTAG:
    //             if (target != null)
    //             {
    //                 // Even though PhotonVision said detected target, FieldLayout may not give us AprilTagPose.
    //                 // Check it before access the AprilTag pose.
    //                 Pose3d aprilTagPose = getAprilTagPose(target.getFiducialId());
    //                 if (aprilTagPose != null)
    //                 {
    //                     targetHeight = aprilTagPose.getZ();
    //                 }
    //             }
    //             break;

    //         case NOTE:
    //             // Assuming Note is on the ground.
    //             targetHeight = 0.0;
    //             break;
    //     }

    //     return targetHeight;
    // }   //getTargetHeight

}   //class PhotonVisionRaw
