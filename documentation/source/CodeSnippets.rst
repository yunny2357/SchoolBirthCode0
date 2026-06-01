程式小片段
==========

.. caution::
    這邊寫的code僅供參考，請勿直接傳到機器上，如果傳到機器上，後果請自行負責


讓馬達動起來吧！
----------------

.. note::
    這邊用Kraken跟NEO的程式來舉例

.. important::
    因為省略掉import的部分，所以直接複製貼上動不了是正常的

.. code-block:: java
    :caption: Defintion

    public SparkMax neo = new SparkMax(10, MotorType.kBrushless);
    public TalonFX kraken = new TalonFX(11, new CANBus()); //new CANBus會指到RIO的bus

.. attention::
    這邊沒做configuration, 關於configuration之後會再做一篇文章做詳細說明（老高

最簡單讓他動起來的方式

.. code-block:: java
    :caption: SimpleRun

    neo.set(1);
    kraken.set(1);

這個方式是讓馬達用開還控制的方式到指定的轉速(趴數，0~1)

不過Kraken更常見的是用這種方式

.. code-block:: java
    :caption: KrakenRun

    public DutyCycleOut request = new DutyCycleOut();

    //---------- (已省略)

    motor.setControl(request.withOutput(1));

這邊會這樣寫別有原因，之後就會懂了

讓我們拉回來，讓我們看看如果整合到系統之後會變成什麼樣子

.. note::
    在這邊因為顧及到編者的心理健康，所以所有程式都只有讓馬達動而已，在實際的程式中會需要更多的工能立如odometry或是auto跟vision

Level 0: 讓馬達動起來
+++++++++++++++++++++

.. tabs::

    .. tab:: Timed Based / SparkMax

        .. code-block:: java
            :linenos:
            
            public class Test extends TimedRobot {

                public SparkMax motor;
                public XboxController controller;

                public Test() {
                    motor = new SparkMax(10, MotorType.kBrushless);
                    controller = new XboxController(0);
                }

                @Override
                public void robotPeriodic() {}

                @Override
                public void disabledInit() {}

                // ... (略)

                @Override
                public void teleopInit() {}

                @Override
                public void teleopPeriodic() {
                    motor.set(controller.getLeftX());
                }

                // ... (略)
            }

    .. tab:: Timed Based / TalonFX (Kraken)

        .. code-block:: java

            public class Test extends TimedRobot {

            public TalonFX motor;
            public DutyCycleOut request;
            public XboxController controller;
            
            public Test() {
                motor = new TalonFX(0, new CANBus());
                request = new DutyCycleOut(0);
                controller = new XboxController(0);
            }

            @Override
            public void robotPeriodic() {
            }

            @Override
            public void disabledInit() {}

            //...(略)

            @Override
            public void teleopInit() {}

            @Override
            public void teleopPeriodic() {
                motor.setControl(request.withOutput(controller.getLeftX()));
            }

            @Override
            public void teleopExit() {}

            // ...略
            }

    .. tab:: Command Based / SparkMAX

        .. code-block:: java
            :linenos:
            :caption: motor.java

            public class motor implements Subsystem{
                public SparkMax motor;

                public motor(){
                    motor = new SparkMax(0, MotorType.kBrushless);
                    this.register();
                }

                public Command runMotor(Supplier<Dimensionless> target){
                    return run(() -> motor.set(target.get().in(Percent)));
                }
            }

        .. code-block:: java
            :linenos:
            :caption: RobotContainer.java

            public class RobotContainer {
                public motor m = new motor();
                public CommandXboxController controller = new CommandXboxController(0);

                public RobotContainer() {
                    configureBindings();
                    m.setDefaultCommand(m.runMotor(() -> Percent.of(controller.getLeftX())));
                }

                private void configureBindings() {
                }

                public Command getAutonomousCommand() {
                    return Commands.print("No autonomous command configured");
                }
            }
    
    .. tab:: Command Based / TalonFX(Kraken)

        .. code-block:: java
            :linenos:
            :caption: motor.java

            public class motor implements Subsystem{
                public TalonFX motor;
                public DutyCycleOut request;

                public motor(){
                    motor = new TalonFX(0, new CANBus);
                    request = new DutyCycleOut(0);
                    this.register();
                }

                public Command runMotor(Supplier<Dimensionless> target){
                    return run(() -> motor.setControl(request.withOutput(target.get().in(Percent))));
                }
            }

        .. code-block:: java
            :linenos:
            :caption: RobotContainer.java

            public class RobotContainer {
                public motor m = new motor();
                public CommandXboxController controller = new CommandXboxController(0);

                public RobotContainer() {
                    configureBindings();
                    m.setDefaultCommand(m.runMotor(() -> Percent.of(controller.getLeftX())));
                }

                private void configureBindings() {
                }

                public Command getAutonomousCommand() {
                    return Commands.print("No autonomous command configured");
                }
            }


這時候你可能會有個疑問，就是明明Timed Based要寫的東西少很多，還不用分兩個檔案，那為什麼還要用Command Based，在這邊我知道你很急，但你先別急，讓我們來慢慢看

現在先把前面的一個小坑補起來，因為Kraken的 :code:`setControl` 有包含到全部他的RequestType, 所以在之後的控制甚至可以用 :code:`TalonFX::setControl` 來餵資料，這個等一下會有示範

.. note:: 
    因為編者為了顧及程式簡潔性(絕對不是因為太懶)，所以之後都用TalonFX來寫


Level 1: KOP 底盤
+++++++++++++++++

.. tabs::

    .. tab:: Timed Based

        .. code-block:: java
            :linenos:
            
            public class Test extends TimedRobot {

                public List<TalonFX> motors;
                public List<TalonFXConfiguration> config;
                public DutyCycleOut driveOutput;
                public Follower followOutput;
                public CommandXboxController controller = new CommandXboxController(0);
                public DifferentialDriveKinematics kinematics;
                public final Distance WheelCirc = Inches.of(6).times(Math.PI);
                public final double GearRatio = 10.71;
                public final LinearVelocity MaxVelocity = MetersPerSecond.of(100/10.71*WheelCirc.in(Meters));

                public Test() {
                    motors = List.of(
                    new TalonFX(10),    //LF
                    new TalonFX(11),   //LB
                    new TalonFX(12),  //RF
                    new TalonFX(13)  //RB
                    );

                    config = List.of(
                    new TalonFXConfiguration()
                        .withMotorOutput(new MotorOutputConfigs()
                        .withNeutralMode(NeutralModeValue.Brake)
                        .withInverted(InvertedValue.Clockwise_Positive))
                        .withFeedback(new FeedbackConfigs()
                        .withSensorToMechanismRatio(GearRatio)),
                        
                    new TalonFXConfiguration()
                        .withMotorOutput(new MotorOutputConfigs()
                        .withNeutralMode(NeutralModeValue.Brake)
                        .withInverted(InvertedValue.CounterClockwise_Positive)),
                    new TalonFXConfiguration()
                        .withMotorOutput(new MotorOutputConfigs()
                        .withNeutralMode(NeutralModeValue.Brake)
                        .withInverted(InvertedValue.Clockwise_Positive))
                        .withFeedback(new FeedbackConfigs()
                        .withSensorToMechanismRatio(GearRatio)),
                    
                    new TalonFXConfiguration()
                        .withMotorOutput(new MotorOutputConfigs()
                        .withNeutralMode(NeutralModeValue.Brake)
                        .withInverted(InvertedValue.CounterClockwise_Positive))
                    );

                    IntStream.range(0, 3).forEach(i -> motors.get(i).getConfigurator().apply(config.get(i)));

                    driveOutput = new DutyCycleOut(0);
                    followOutput = new Follower(0, MotorAlignmentValue.Aligned);
                    kinematics = new DifferentialDriveKinematics(Inches.of(10));
                }

                // ... (略)

                @Override
                public void teleopInit() {
                }

                @Override
                public void teleopPeriodic() {
                    ChassisSpeeds targetSpeeds = new ChassisSpeeds(controller.getLeftX(), controller.getLeftY(), controller.getRightX());
                    DifferentialDriveWheelSpeeds speeds = kinematics.toWheelSpeeds(targetSpeeds);
                    
                    motors.get(0).setControl(driveOutput.withOutput(speeds.leftMetersPerSecond/MaxVelocity.in(MetersPerSecond)));
                    motors.get(1).setControl(followOutput.withLeaderID(motors.get(0).getDeviceID()));
                    motors.get(2).setControl(driveOutput.withOutput(speeds.rightMetersPerSecond/MaxVelocity.in(MetersPerSecond)));
                    motors.get(3).setControl(followOutput.withLeaderID(motors.get(2).getDeviceID()));
                }

                @Override
                public void teleopExit() {}

                // ..(略)
            }

    .. tab:: Command Based

        .. code-block:: java
            :linenos:
            :caption: Drivetrain.java

            public class Drivetrain implements Subsystem{
                public TalonFX FrontLeft, FrontRight, BackLeft, BackRight;
                public TalonFXConfiguration FLConfig, FRConfig, BLConfig, BRConfig;
                public DutyCycleOut output;
                public Follower follower;
                public DifferentialDriveKinematics kinematics;
                public final Distance WheelCirc = Inches.of(6).times(Math.PI);
                public final double GearRatio = 10.71;
                public final double MaxVelocity = 100/10.71*WheelCirc.in(Meters);

                public Drivetrain(){
                    FrontLeft = new TalonFX(0);
                    FrontRight = new TalonFX(1);
                    BackLeft = new TalonFX(2);
                    BackRight = new TalonFX(3);

                    FLConfig = new TalonFXConfiguration()
                        .withMotorOutput(new MotorOutputConfigs()
                            .withNeutralMode(NeutralModeValue.Brake)
                            .withInverted(InvertedValue.Clockwise_Positive))
                        .withFeedback(new FeedbackConfigs()
                            .withSensorToMechanismRatio(GearRatio));

                    FRConfig = new TalonFXConfiguration()
                        .withMotorOutput(new MotorOutputConfigs()
                            .withNeutralMode(NeutralModeValue.Brake)
                            .withInverted(InvertedValue.CounterClockwise_Positive))
                        .withFeedback(new FeedbackConfigs()
                            .withSensorToMechanismRatio(GearRatio));

                    BLConfig = new TalonFXConfiguration()
                        .withMotorOutput(new MotorOutputConfigs()
                            .withNeutralMode(NeutralModeValue.Brake)
                            .withInverted(InvertedValue.Clockwise_Positive))
                        .withFeedback(new FeedbackConfigs()
                            .withSensorToMechanismRatio(GearRatio));

                    BRConfig = new TalonFXConfiguration()
                        .withMotorOutput(new MotorOutputConfigs()
                            .withNeutralMode(NeutralModeValue.Brake)
                            .withInverted(InvertedValue.CounterClockwise_Positive))
                        .withFeedback(new FeedbackConfigs()
                            .withSensorToMechanismRatio(GearRatio));

                    FrontLeft.getConfigurator().apply(FLConfig);
                    FrontRight.getConfigurator().apply(FRConfig);
                    BackLeft.getConfigurator().apply(BLConfig);
                    BackRight.getConfigurator().apply(BRConfig);

                    output = new DutyCycleOut(0);
                    follower = new Follower(0, MotorAlignmentValue.Aligned);
                    kinematics = new DifferentialDriveKinematics(0.0); 
                }

                public Command drive(Supplier<ChassisSpeeds> speeds){
                    return run( () -> {
                        DifferentialDriveWheelSpeeds wheelspeed = kinematics.toWheelSpeeds(speeds.get());
                        FrontLeft.setControl(output.withOutput(wheelspeed.leftMetersPerSecond/MaxVelocity));
                        FrontRight.setControl(output.withOutput(wheelspeed.rightMetersPerSecond/MaxVelocity));
                        BackLeft.setControl(follower.withLeaderID(FrontLeft.getDeviceID()));
                        BackRight.setControl(follower.withLeaderID(FrontRight.getDeviceID()));
                    });
                }
            }

        .. code-block:: java
            :linenos:
            :caption: RobotContainer.java

            public class RobotContainer {
                public Drivetrain drivetrain = new Drivetrain();
                public CommandXboxController controller = new CommandXboxController(0);

                public RobotContainer() {
                    configureBindings();
                    drivetrain.setDefaultCommand(drivetrain.drive(() -> new ChassisSpeeds(
                    controller.getLeftX() * drivetrain.MaxVelocity,
                    controller.getRightX() * drivetrain.MaxVelocity,
                    controller.getRightX() * 10 //max omega in rad/s
                    )));
                }

                private void configureBindings() {
                }

                public Command getAutonomousCommand() {
                    return Commands.print("No autonomous command configured");
                }
            }

你說你這樣還是沒感覺，那接下來應該就會有感覺了

Level 2: 向量底盤
+++++++++++++++++

.. warning::
    這邊只有做教學用途才會這樣寫，請在寫正式的code的時後乖乖用Command Based, 不然在比賽中突然程式爆掉在debug的你會想把現在的你殺掉

.. tabs::

    .. tab:: Command Based

        .. code-block:: java
            :linenos:
            :caption: SwerveMod.java

            public class SwerveMod {
                public TalonFX DriveMotor, SteerMotor;
                public CANcoder encoder;
                public MotionMagicVelocityTorqueCurrentFOC DriveRequest;
                public MotionMagicExpoTorqueCurrentFOC SteerRequest;

                private TalonFXConfiguration DriveConfig, SteerConfig;
                private CANcoderConfiguration EncoderConfig;

                public SwerveMod(int DriveID, int SteerID, int EncoderID, Angle offset, boolean isLeftSide){
                    DriveMotor = new TalonFX(DriveID);
                    SteerMotor = new TalonFX(SteerID);
                    encoder = new CANcoder(EncoderID);

                    DriveRequest = new MotionMagicVelocityTorqueCurrentFOC(0);
                    SteerRequest = new MotionMagicExpoTorqueCurrentFOC(0);

                    DriveConfig = new TalonFXConfiguration();
                    SteerConfig = new TalonFXConfiguration();
                    EncoderConfig = new CANcoderConfiguration();

                    DriveConfig.MotorOutput
                        .withInverted(isLeftSide ? InvertedValue.Clockwise_Positive : InvertedValue.CounterClockwise_Positive)
                        .withNeutralMode(NeutralModeValue.Brake);
                    DriveConfig.Feedback
                        .withSensorToMechanismRatio(Constants.DriveGearRatio);
                    DriveConfig.Slot0
                        .withKP(0).withKD(0)
                        .withStaticFeedforwardSign(StaticFeedforwardSignValue.UseVelocitySign);
                    DriveConfig.CurrentLimits
                        .withStatorCurrentLimit(Constants.SlipCurrent)
                        .withStatorCurrentLimitEnable(true);
                    DriveConfig.MotionMagic
                        .withMotionMagicAcceleration(0);
                    
                    SteerConfig.MotorOutput
                        .withInverted(InvertedValue.Clockwise_Positive)
                        .withNeutralMode(NeutralModeValue.Brake);
                    DriveConfig.Feedback
                        .withFusedCANcoder(encoder)
                        .withRotorToSensorRatio(Constants.SteerGearRatio);
                    DriveConfig.Slot0
                        .withKP(0).withKD(0)
                        .withStaticFeedforwardSign(StaticFeedforwardSignValue.UseVelocitySign);
                    DriveConfig.CurrentLimits
                        .withStatorCurrentLimit(Amps.of(60))
                        .withStatorCurrentLimitEnable(true);
                    DriveConfig.MotionMagic
                        .withMotionMagicExpo_kV(0)
                        .withMotionMagicExpo_kA(0);

                    EncoderConfig.MagnetSensor
                        .withMagnetOffset(offset);

                    DriveMotor.getConfigurator().apply(DriveConfig);
                    SteerMotor.getConfigurator().apply(SteerConfig);
                    encoder.getConfigurator().apply(EncoderConfig);
                }

                public SwerveModulePosition getPosition(){
                    return new SwerveModulePosition(
                        DriveMotor.getPosition().getValueAsDouble() * Constants.WheelRadius.times(2*Math.PI).in(Meters),
                        new Rotation2d(SteerMotor.getPosition().getValue())
                    );
                }

                public SwerveModuleState getState(){
                    return new SwerveModuleState(
                        DriveMotor.getVelocity().getValueAsDouble() * Constants.WheelRadius.times(2*Math.PI).in(Meters),
                        new Rotation2d(SteerMotor.getPosition().getValue())
                    );
                }

                public void setState(SwerveModuleState state){
                    state.optimize(getState().angle);

                    DriveMotor.setControl(DriveRequest.withVelocity(state.speedMetersPerSecond/Constants.WheelRadius.times(2*Math.PI).in(Meters)));
                    SteerMotor.setControl(SteerRequest.withPosition(state.angle.getMeasure()));
                }
            }


        .. code-block:: java
            :linenos:
            :caption: Drivetrain.java

            public class Drivetrain implements Subsystem{
                public List<SwerveMod> modules;
                public Pigeon2 gyro;
                public SwerveDriveKinematics kinematics;

                public Drivetrain(){
                    modules = List.of(
                        new SwerveMod(FrontLeft.DriveID, FrontLeft.SteerID, FrontLeft.EncoderID, FrontLeft.Offset, true),
                        new SwerveMod(FrontRight.DriveID, FrontRight.SteerID, FrontRight.EncoderID, FrontRight.Offset, false),
                        new SwerveMod(BackLeft.DriveID, BackLeft.SteerID, BackLeft.EncoderID, BackLeft.Offset, true),
                        new SwerveMod(BackRight.DriveID, BackRight.SteerID, BackRight.EncoderID, BackRight.Offset, false)
                    );

                    gyro = new Pigeon2(0);
                    kinematics = new SwerveDriveKinematics(FrontLeft.place, FrontRight.place, BackLeft.place, BackRight.place);
                }

                public Command drive(Supplier<LinearVelocity> vx, Supplier<LinearVelocity> vy, Supplier<AngularVelocity> omega){
                    return run(() -> setState(kinematics.toSwerveModuleStates(new ChassisSpeeds(vx.get(), vy.get(), omega.get()))));
                }
                
                private void setState(SwerveModuleState[] states){
                    IntStream.range(0, 4).forEach(i -> modules.get(i).setState(states[i]));
                }
            }

        .. code-block:: java
            :linenos:
            :caption: RobotContainer.java

            public class RobotContainer {
                public CommandXboxController controller = new CommandXboxController(0);
                public Drivetrain drivetrain = new Drivetrain();

                public RobotContainer() {
                    drivetrain.setDefaultCommand(drivetrain.drive(
                    () -> Constants.MaxVelocity.times(controller.getLeftX()), 
                    () -> Constants.MaxVelocity.times(controller.getLeftY()), 
                    () -> Constants.MaxOmega.times(controller.getRightX())));
                }

                private void configureBindings() {
                }

                public Command getAutonomousCommand() {
                    return Commands.print("No autonomous command configured");
                }
            }
    
    .. tab:: Timed Based

        .. code-block:: java
            :linenos:

            public class Test extends TimedRobot {
            
                public List<TalonFX> DriveMotors, SteerMotors;
                public List<CANcoder> Encoders;
                public TalonFXConfiguration DriveConfig, SteerConfig;
                public CANcoderConfiguration EncoderConfig;
                public SwerveDriveKinematics kinematics;
                public List<Angle> EncoderOffset;
                public Pigeon2 gyro;
                public MotionMagicVelocityTorqueCurrentFOC DriveOutput;
                public MotionMagicExpoTorqueCurrentFOC SteerOutput;
                public LinearVelocity MaxVelocity = MetersPerSecond.of(5);
                public AngularVelocity MaxOmega = RotationsPerSecond.of(1.5);
                public Distance WheelCirc = Inches.of(4).times(Math.PI);
                public CommandXboxController controller;
                

                public Test() {
                    DriveMotors = List.of(
                    new TalonFX(11),
                    new TalonFX(21),
                    new TalonFX(31),
                    new TalonFX(41)
                    );

                    SteerMotors = List.of(
                    new TalonFX(12),
                    new TalonFX(22),
                    new TalonFX(32),
                    new TalonFX(42)
                    );

                    Encoders = List.of(
                    new CANcoder(10),
                    new CANcoder(2),
                    new CANcoder(3),
                    new CANcoder(4)
                    );

                    EncoderOffset = List.of(
                    Rotations.of(0),
                    Rotations.of(0),
                    Rotations.of(0),
                    Rotations.of(0)
                    );

                    DriveConfig = new TalonFXConfiguration();
                    SteerConfig = new TalonFXConfiguration();
                    EncoderConfig = new CANcoderConfiguration();

                    DriveConfig.MotorOutput
                    .withInverted(InvertedValue.Clockwise_Positive)
                    .withNeutralMode(NeutralModeValue.Brake);
                    DriveConfig.Feedback
                    .withSensorToMechanismRatio(6.12);
                    DriveConfig.Slot0
                    .withKP(0).withKD(0)
                    .withStaticFeedforwardSign(StaticFeedforwardSignValue.UseVelocitySign);
                    DriveConfig.MotionMagic
                    .withMotionMagicAcceleration(0);
                    DriveConfig.CurrentLimits
                    .withStatorCurrentLimit(Amps.of(120))
                    .withStatorCurrentLimitEnable(true);
                    
                    SteerConfig.MotorOutput
                    .withInverted(InvertedValue.Clockwise_Positive)
                    .withNeutralMode(NeutralModeValue.Brake);
                    SteerConfig.Slot0
                    .withKP(0).withKD(0);
                    SteerConfig.MotionMagic
                    .withMotionMagicExpo_kV(0)
                    .withMotionMagicExpo_kA(0);
                    SteerConfig.CurrentLimits
                    .withStatorCurrentLimit(Amps.of(40))
                    .withStatorCurrentLimitEnable(true);
                    
                    DriveMotors.stream().forEach(m -> m.getConfigurator().apply(DriveConfig));
                    SteerMotors.stream().forEach(m -> m.getConfigurator().apply(SteerConfig));
                    Encoders.stream().forEach(e -> e.getConfigurator().apply(EncoderConfig));
                    IntStream.range(0, 4).forEach(i -> SteerMotors.get(i).getConfigurator().apply(new FeedbackConfigs()
                    .withRotorToSensorRatio(150.0/7)
                    .withFusedCANcoder(Encoders.get(i))));
                    IntStream.range(0, 3).forEach(i -> Encoders.get(i).getConfigurator().apply(new MagnetSensorConfigs().withSensorDirection(SensorDirectionValue.Clockwise_Positive).withMagnetOffset(EncoderOffset.get(i))));

                    kinematics = new SwerveDriveKinematics(
                    new Translation2d(0,0),
                    new Translation2d(0,0),
                    new Translation2d(0,0),
                    new Translation2d(0,0)
                    );

                    gyro = new Pigeon2(0);
                    DriveOutput = new MotionMagicVelocityTorqueCurrentFOC(0);
                    SteerOutput = new MotionMagicExpoTorqueCurrentFOC(0);
                    controller = new CommandXboxController(0);
                }

                // ...略

                @Override
                public void teleopInit() {
                    
                }

                @Override
                public void teleopPeriodic() {
                    List<SwerveModuleState> target = List.of(kinematics.toSwerveModuleStates(new ChassisSpeeds(MaxVelocity.times(controller.getLeftX()), MaxVelocity.times(controller.getLeftY()), MaxOmega.times(controller.getRightX()))));

                    IntStream.range(0, 4).forEach(i -> {
                    DriveMotors.get(i).setControl(DriveOutput.withVelocity(target.get(i).speedMetersPerSecond/WheelCirc.in(Meters)));
                    SteerMotors.get(i).setControl(SteerOutput.withPosition(target.get(i).angle.getMeasure()));
                    });
                }

                // ...略
            }

Level 3: 完整的機器程式

.. caution:: 
    在這邊，如果你有想要用Timed Based的想法的話，請撥打電話1995，謝謝

`範例程式 <https://github.com/FRC-11177/SchoolBirthCode/tree/ExampleRobotCode>`_
