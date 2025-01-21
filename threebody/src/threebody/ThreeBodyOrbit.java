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
 * �Ľ�������ģ�⣺
 * 1. ʹ�������� (r^2 -> r^2 + EPS^2) ���⼫�˽�������ײ���µ���ֵ��ը��
 * 2. ��ʱ�䲽�� DT �� 0.1 ��СΪ 0.05 (�ɸ�����Ҫ��һ����С)��
 * 3. �����߼�(��ϲ���ײ��������ײ)��δʵ�֣��ɰ�����������չ��
 */
public class ThreeBodyOrbit extends Application {

    // =================== ģ����� ===================
    private static final double G = 6.67;       // �������� (���ֽϴ�)
    private static final double DT = 0.1;      // ��Сʱ�䲽�� (��ԭ�� 0.1 ��С)
    private static final int STEPS_PER_FRAME = 10;
    private static final int MAX_TRAIL_POINTS = 1000;

    private static final double WIDTH = 1200;
    private static final double HEIGHT = 800;

    // ���������� (Խ������ײԽ���׷�����������΢��)
    private static final double EPS = 10.0;

    // �Ƿ��ڿ���̨�������
    private static final boolean MONITOR_ENERGY = true;
    private long frameCount = 0;

    // =================== ���ݽṹ ===================
    private final List<Body> bodies = new ArrayList<>();
    private final List<Circle> circles = new ArrayList<>();
    private final List<Polyline> trails = new ArrayList<>();

    // ���� Velocity Verlet
    private double[] axOld;
    private double[] ayOld;

    @Override
    public void start(Stage primaryStage) {
        Group root = new Group();
        Scene scene = new Scene(root, WIDTH, HEIGHT, Color.BLACK);

        // �ô���������Ϊ��ʼ�ο���
        double centerX = WIDTH / 2.0;
        double centerY = HEIGHT / 2.0;

        // ������
        double mass = 13.0;
        double R = 150.0;
        double v0 = 1;  // ��ʼ�ٶȱ���

        // �������� (�ȱ�������)
        double x1 = centerX;
        double y1 = centerY - R;
        double x2 = centerX + (Math.sqrt(3)/2)*R;
        double y2 = centerY + R/2.0;
        double x3 = centerX - (Math.sqrt(3)/2)*R;
        double y3 = centerY + R/2.0;

        // ��΢�ƻ��Գ�
        Body b1 = new Body(x1, y1, 0.9*v0, 0, mass, Color.RED);
        Body b2 = new Body(x2, y2, v0, 0.33*v0, mass, Color.CYAN);
        Body b3 = new Body(x3, y3, 0.33*v0, -0.63*v0, mass, Color.YELLOW);

        bodies.add(b1);
        bodies.add(b2);
        bodies.add(b3);

        // ��¼���ٶȵ�����
        axOld = new double[bodies.size()];
        ayOld = new double[bodies.size()];

        // =================== ���ӻ��ڵ� ===================
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

        // ��ʼ���ٶ� (����)
        computeAcceleration(bodies, axOld, ayOld);

        // ����ѭ��
        AnimationTimer timer = new AnimationTimer() {
            @Override
            
            public void handle(long now) {
                // 1) ��ε���
            	// �������� bodies λ����켣֮��
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

                // 2) ���¿��ӻ� (λ��, �켣)
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

                // 3) �������(��ѡ)
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
     * ʹ�� Velocity Verlet ���ָ���
     */
    private void velocityVerletUpdate(List<Body> bodies, double[] axOld, double[] ayOld, double dt) {
        // 1) ����һ�εļ��ٶȸ���λ��
        for (int i = 0; i < bodies.size(); i++) {
            Body b = bodies.get(i);
            b.x += b.vx * dt + 0.5 * axOld[i] * dt * dt;
            b.y += b.vy * dt + 0.5 * ayOld[i] * dt * dt;
            b.vx += 0.5 * axOld[i] * dt;
            b.vy += 0.5 * ayOld[i] * dt;
        }

        // 2) �����µļ��ٶ�(����)
        double[] axNew = new double[bodies.size()];
        double[] ayNew = new double[bodies.size()];
        computeAcceleration(bodies, axNew, ayNew);

        // 3) �ٴθ����ٶ�
        for (int i = 0; i < bodies.size(); i++) {
            Body b = bodies.get(i);
            b.vx += 0.5 * axNew[i] * dt;
            b.vy += 0.5 * ayNew[i] * dt;
        }

        // ���Ǿɼ��ٶ�
        System.arraycopy(axNew, 0, axOld, 0, axNew.length);
        System.arraycopy(ayNew, 0, ayOld, 0, ayNew.length);
    }

    /**
     * ����ÿ������ļ��ٶ� (������)
     */
    private void computeAcceleration(List<Body> bodies, double[] ax, double[] ay) {
        // ����
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
                // 1) �ھ���ƽ������� eps^2
                double softDistSq = distSq + EPS*EPS;
                double dist = Math.sqrt(softDistSq);

                // 2) �����������
                double force = G * bi.mass * bj.mass / softDistSq;

                // 3) �ֽ⵽ x,y
                ax[i] += force * (dx / dist) / bi.mass;
                ay[i] += force * (dy / dist) / bi.mass;
            }
        }
    }

    /**
     * ����ϵͳ������ (���� + ����)
     */
    private double computeEnergy(List<Body> bodies) {
        double kinetic = 0;
        double potential = 0;

        // 1) ����
        for (Body b : bodies) {
            double v2 = b.vx*b.vx + b.vy*b.vy;
            kinetic += 0.5 * b.mass * v2;
        }

        // 2) ���� (����)
        for (int i = 0; i < bodies.size(); i++) {
            for (int j = i+1; j < bodies.size(); j++) {
                Body bi = bodies.get(i);
                Body bj = bodies.get(j);
                double dx = bi.x - bj.x;
                double dy = bi.y - bj.y;
                double r2 = dx*dx + dy*dy + EPS*EPS; // ����ͬ���ɴ���
                double r = Math.sqrt(r2);

                potential += - G * bi.mass * bj.mass / r2 * r; 
                // ���߸�ֱ�ӣ� potential += - (G * mi * mj) / sqrt(r2)
                // ������ʾ����EPS��Ӱ�죬���⼫Сrʱ���ܹ��ȷ�ɢ
            }
        }
        return kinetic + potential;
    }

    /**
     * ������
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
