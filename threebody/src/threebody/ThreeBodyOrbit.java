package threebody;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * 改进版三体模拟：
 * 1. 使用引力软化 (r^2 -> r^2 + EPS^2) 避免极端近距离碰撞导致的数值爆炸。
 * 2. 将时间步长 DT 从 0.1 调小为 0.05 (可根据需要进一步减小)。
 * 3. 其余逻辑(如合并碰撞、弹性碰撞)暂未实现，可按需求自行扩展。
 */
public class ThreeBodyOrbit extends Application {

    // =================== 模拟参数 ===================
    private static final double G = 6.67;       // 引力常数 (保持较大)
    private static final double DT = 0.1;      // 减小时间步长 (比原来 0.1 更小)
    private static final int STEPS_PER_FRAME = 10;
    private static final int MAX_TRAIL_POINTS = 1000;

    private static final double WIDTH = 1200;
    private static final double HEIGHT = 800;

    // 引力软化长度 (越大则碰撞越不易发生；可自行微调)
    private static final double EPS = 10.0;

    // 是否在控制台监控能量
    private static final boolean MONITOR_ENERGY = true;
    private long frameCount = 0;

    // =================== 数据结构 ===================
    private final List<Body> bodies = new ArrayList<>();
    private final List<Circle> circles = new ArrayList<>();
    private final List<Polyline> trails = new ArrayList<>();

    // 用于 Velocity Verlet
    private double[] axOld;
    private double[] ayOld;

    @Override
    public void start(Stage primaryStage) {
        Group root = new Group();
        Scene scene = new Scene(root, WIDTH, HEIGHT, Color.BLACK);

        // 让窗口中心作为初始参考点
        double centerX = WIDTH / 2.0;
        double centerY = HEIGHT / 2.0;

        // 三颗星
        double mass = 13.0;
        double R = 150.0;
        double v0 = 1;  // 初始速度标量

        // 三个顶点 (等边三角形)
        double x1 = centerX;
        double y1 = centerY - R;
        double x2 = centerX + (Math.sqrt(3)/2)*R;
        double y2 = centerY + R/2.0;
        double x3 = centerX - (Math.sqrt(3)/2)*R;
        double y3 = centerY + R/2.0;

        // 略微破坏对称
        Body b1 = new Body(x1, y1, 0.9*v0, 0, mass, Color.RED);
        Body b2 = new Body(x2, y2, v0, 0.33*v0, mass, Color.CYAN);
        Body b3 = new Body(x3, y3, 0.33*v0, -0.63*v0, mass, Color.YELLOW);

        bodies.add(b1);
        bodies.add(b2);
        bodies.add(b3);

        // 记录加速度的数组
        axOld = new double[bodies.size()];
        ayOld = new double[bodies.size()];

        // =================== 可视化节点 ===================
        for (Body b : bodies) {
            Circle c = new Circle(0, 0, 6, b.color);
            circles.add(c);
            root.getChildren().add(c);

            Polyline trail = new Polyline();
            trail.setStroke(b.color.deriveColor(1, 1, 1, 0.7));
            trail.setStrokeWidth(2);
            trails.add(trail);
            root.getChildren().add(trail);
        }

        // 初始加速度 (带软化)
        computeAcceleration(bodies, axOld, ayOld);

        // 动画循环
        AnimationTimer timer = new AnimationTimer() {
            @Override
            
            public void handle(long now) {
                // 1) 多次迭代
            	// （更新完 bodies 位置与轨迹之后）
            	double cmX = 0, cmY = 0, totalMass = 0;
            	for (Body b : bodies) {
            	    cmX += b.x * b.mass;
            	    cmY += b.y * b.mass;
            	    totalMass += b.mass;
            	}
            	cmX /= totalMass;
            	cmY /= totalMass;

            	root.setTranslateX(centerX - cmX);
            	root.setTranslateY(centerY - cmY);

                for (int s = 0; s < STEPS_PER_FRAME; s++) {
                    velocityVerletUpdate(bodies, axOld, ayOld, DT);
                }

                // 2) 更新可视化 (位置, 轨迹)
                for (int i = 0; i < bodies.size(); i++) {
                    Body b = bodies.get(i);
                    circles.get(i).setCenterX(b.x);
                    circles.get(i).setCenterY(b.y);

                    Polyline trail = trails.get(i);
                    trail.getPoints().addAll(b.x, b.y);
                    if (trail.getPoints().size() > 2 * MAX_TRAIL_POINTS) {
                        trail.getPoints().remove(0, 2);
                    }
                }

                // 3) 监控能量(可选)
                frameCount++;
                if (MONITOR_ENERGY && frameCount % 200 == 0) {
                    double totalE = computeEnergy(bodies);
                    System.out.printf("Frame=%d, Total Energy=%.5f\n", frameCount, totalE);
                }
            }
            
        };
        timer.start();

        primaryStage.setTitle("Three-Body with Softening (Avoid Collisions)");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * 使用 Velocity Verlet 积分更新
     */
    private void velocityVerletUpdate(List<Body> bodies, double[] axOld, double[] ayOld, double dt) {
        // 1) 用上一次的加速度更新位置
        for (int i = 0; i < bodies.size(); i++) {
            Body b = bodies.get(i);
            b.x += b.vx * dt + 0.5 * axOld[i] * dt * dt;
            b.y += b.vy * dt + 0.5 * ayOld[i] * dt * dt;
            b.vx += 0.5 * axOld[i] * dt;
            b.vy += 0.5 * ayOld[i] * dt;
        }

        // 2) 计算新的加速度(带软化)
        double[] axNew = new double[bodies.size()];
        double[] ayNew = new double[bodies.size()];
        computeAcceleration(bodies, axNew, ayNew);

        // 3) 再次更新速度
        for (int i = 0; i < bodies.size(); i++) {
            Body b = bodies.get(i);
            b.vx += 0.5 * axNew[i] * dt;
            b.vy += 0.5 * ayNew[i] * dt;
        }

        // 覆盖旧加速度
        System.arraycopy(axNew, 0, axOld, 0, axNew.length);
        System.arraycopy(ayNew, 0, ayOld, 0, ayNew.length);
    }

    /**
     * 计算每个天体的加速度 (引力软化)
     */
    private void computeAcceleration(List<Body> bodies, double[] ax, double[] ay) {
        // 清零
        for (int i = 0; i < bodies.size(); i++) {
            ax[i] = 0;
            ay[i] = 0;
        }

        for (int i = 0; i < bodies.size(); i++) {
            Body bi = bodies.get(i);
            for (int j = 0; j < bodies.size(); j++) {
                if (i == j) continue;
                Body bj = bodies.get(j);

                double dx = bj.x - bi.x;
                double dy = bj.y - bi.y;
                double distSq = dx*dx + dy*dy;
                // 1) 在距离平方上添加 eps^2
                double softDistSq = distSq + EPS*EPS;
                double dist = Math.sqrt(softDistSq);

                // 2) 计算带软化的力
                double force = G * bi.mass * bj.mass / softDistSq;

                // 3) 分解到 x,y
                ax[i] += force * (dx / dist) / bi.mass;
                ay[i] += force * (dy / dist) / bi.mass;
            }
        }
    }

    /**
     * 计算系统总能量 (动能 + 势能)
     */
    private double computeEnergy(List<Body> bodies) {
        double kinetic = 0;
        double potential = 0;

        // 1) 动能
        for (Body b : bodies) {
            double v2 = b.vx*b.vx + b.vy*b.vy;
            kinetic += 0.5 * b.mass * v2;
        }

        // 2) 势能 (两两)
        for (int i = 0; i < bodies.size(); i++) {
            for (int j = i+1; j < bodies.size(); j++) {
                Body bi = bodies.get(i);
                Body bj = bodies.get(j);
                double dx = bi.x - bj.x;
                double dy = bi.y - bj.y;
                double r2 = dx*dx + dy*dy + EPS*EPS; // 势能同样可带软化
                double r = Math.sqrt(r2);

                potential += - G * bi.mass * bj.mass / r2 * r; 
                // 或者更直接： potential += - (G * mi * mj) / sqrt(r2)
                // 这里演示保留EPS的影响，避免极小r时势能过度发散
            }
        }
        return kinetic + potential;
    }

    /**
     * 天体类
     */
    public static class Body {
        double x;
        double y;
        double vx;
        double vy;
        double mass;
        Color color;

        public Body(double x, double y, double vx, double vy, double mass, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.mass = mass;
            this.color = color;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
