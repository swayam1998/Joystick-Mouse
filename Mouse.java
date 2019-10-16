import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import gnu.io.CommPortIdentifier; 
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent; 
import gnu.io.SerialPortEventListener; 
import java.util.Enumeration;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.CompoundControl;
import javax.sound.sampled.Control;
import javax.sound.sampled.Control.Type;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;



public class Mouse implements SerialPortEventListener {
    SerialPort serialPort;
        /** The port we're normally going to use. */
    private static final String PORT_NAMES[] = {"/dev/ttyUSB1","/dev/ttyUSB0","/dev/ttyUSB2","/dev/ttyUSB3","/dev/ttyUSB4","/dev/ttyUSB5"};
    /**
    * A BufferedReader which will be fed by a InputStreamReader 
    * converting the bytes into characters 
    * making the displayed results codepage independent
    */
    private BufferedReader input;
    /** The output stream to the port */
    private OutputStream output;
    /** Milliseconds to block while waiting for port open */
    private static final int TIME_OUT = 2000;
    /** Default bits per second for COM port. */
    private static final int DATA_RATE = 9600;
    
    int buttonOld = 1;
    
    public class Audio {

    public void main(String[] args) throws Exception {
        System.out.println(getHierarchyInfo());
        System.out.println(getMasterOutputVolume());
    }

    public void setMasterOutputVolume(float value) {
        if (value < 0 || value > 1)
            throw new IllegalArgumentException(
                    "Volume can only be set to a value from 0 to 1. Given value is illegal: " + value);
        Line line = getMasterOutputLine();
        if (line == null) throw new RuntimeException("Master output port not found");
        boolean opened = open(line);
        try {
            FloatControl control = getVolumeControl(line);
            if (control == null)
                throw new RuntimeException("Volume control not found in master port: " + toString(line));
            control.setValue(value);
        } finally {
            if (opened) line.close();
        }
    }

    public Float getMasterOutputVolume() {
        Line line = getMasterOutputLine();
        if (line == null) return null;
        boolean opened = open(line);
        try {
            FloatControl control = getVolumeControl(line);
            if (control == null) return null;
            return control.getValue();
        } finally {
            if (opened) line.close();
        }
    }

    public void setMasterOutputMute(boolean value) {
        Line line = getMasterOutputLine();
        if (line == null) throw new RuntimeException("Master output port not found");
        boolean opened = open(line);
        try {
            BooleanControl control = getMuteControl(line);
            if (control == null)
                throw new RuntimeException("Mute control not found in master port: " + toString(line));
            control.setValue(value);
        } finally {
            if (opened) line.close();
        }
    }

    public Boolean getMasterOutputMute() {
        Line line = getMasterOutputLine();
        if (line == null) return null;
        boolean opened = open(line);
        try {
            BooleanControl control = getMuteControl(line);
            if (control == null) return null;
            return control.getValue();
        } finally {
            if (opened) line.close();
        }
    }

    public Line getMasterOutputLine() {
        for (Mixer mixer : getMixers()) {
            for (Line line : getAvailableOutputLines(mixer)) {
                if (line.getLineInfo().toString().contains("Master")) return line;
            }
        }
        return null;
    }

    public FloatControl getVolumeControl(Line line) {
        if (!line.isOpen()) throw new RuntimeException("Line is closed: " + toString(line));
        return (FloatControl) findControl(FloatControl.Type.VOLUME, line.getControls());
    }

    public BooleanControl getMuteControl(Line line) {
        if (!line.isOpen()) throw new RuntimeException("Line is closed: " + toString(line));
        return (BooleanControl) findControl(BooleanControl.Type.MUTE, line.getControls());
    }

    private Control findControl(Type type, Control... controls) {
        if (controls == null || controls.length == 0) return null;
        for (Control control : controls) {
            if (control.getType().equals(type)) return control;
            if (control instanceof CompoundControl) {
                CompoundControl compoundControl = (CompoundControl) control;
                Control member = findControl(type, compoundControl.getMemberControls());
                if (member != null) return member;
            }
        }
        return null;
    }

    public List<Mixer> getMixers() {
        Info[] infos = AudioSystem.getMixerInfo();
        List<Mixer> mixers = new ArrayList<Mixer>(infos.length);
        for (Info info : infos) {
            Mixer mixer = AudioSystem.getMixer(info);
            mixers.add(mixer);
        }
        return mixers;
    }

    public List<Line> getAvailableOutputLines(Mixer mixer) {
        return getAvailableLines(mixer, mixer.getTargetLineInfo());
    }

    public List<Line> getAvailableInputLines(Mixer mixer) {
        return getAvailableLines(mixer, mixer.getSourceLineInfo());
    }

    private List<Line> getAvailableLines(Mixer mixer, Line.Info[] lineInfos) {
        List<Line> lines = new ArrayList<Line>(lineInfos.length);
        for (Line.Info lineInfo : lineInfos) {
            Line line;
            line = getLineIfAvailable(mixer, lineInfo);
            if (line != null) lines.add(line);
        }
        return lines;
    }

    public Line getLineIfAvailable(Mixer mixer, Line.Info lineInfo) {
        try {
            return mixer.getLine(lineInfo);
        } catch (LineUnavailableException ex) {
            return null;
        }
    }

    public String getHierarchyInfo() {
        StringBuilder sb = new StringBuilder();
        for (Mixer mixer : getMixers()) {
            sb.append("Mixer: ").append(toString(mixer)).append("\n");

            for (Line line : getAvailableOutputLines(mixer)) {
                sb.append("  OUT: ").append(toString(line)).append("\n");
                boolean opened = open(line);
                for (Control control : line.getControls()) {
                    sb.append("    Control: ").append(toString(control)).append("\n");
                    if (control instanceof CompoundControl) {
                        CompoundControl compoundControl = (CompoundControl) control;
                        for (Control subControl : compoundControl.getMemberControls()) {
                            sb.append("      Sub-Control: ").append(toString(subControl)).append("\n");
                        }
                    }
                }
                if (opened) line.close();
            }

            for (Line line : getAvailableOutputLines(mixer)) {
                sb.append("  IN: ").append(toString(line)).append("\n");
                boolean opened = open(line);
                for (Control control : line.getControls()) {
                    sb.append("    Control: ").append(toString(control)).append("\n");
                    if (control instanceof CompoundControl) {
                        CompoundControl compoundControl = (CompoundControl) control;
                        for (Control subControl : compoundControl.getMemberControls()) {
                            sb.append("      Sub-Control: ").append(toString(subControl)).append("\n");
                        }
                    }
                }
                if (opened) line.close();
            }

            sb.append("\n");
        }
        return sb.toString();
    }

    public boolean open(Line line) {
        if (line.isOpen()) return false;
        try {
            line.open();
        } catch (LineUnavailableException ex) {
            return false;
        }
        return true;
    }

    public String toString(Control control) {
        if (control == null) return null;
        return control.toString() + " (" + control.getType().toString() + ")";
    }

    public String toString(Line line) {
        if (line == null) return null;
        Line.Info info = line.getLineInfo();
        return info.toString();// + " (" + line.getClass().getSimpleName() + ")";
    }

    public String toString(Mixer mixer) {
        if (mixer == null) return null;
        StringBuilder sb = new StringBuilder();
        Info info = mixer.getMixerInfo();
        sb.append(info.getName());
        sb.append(" (").append(info.getDescription()).append(")");
        sb.append(mixer.isOpen() ? " [open]" : " [closed]");
        return sb.toString();
    }

}

    
    public void initialize() {
                // the next line is for Raspberry Pi and 
                // gets us into the while loop and was suggested here was suggested  http://www.raspberrypi.org/phpBB3/viewtopic.php?f...
                //System.setProperty("gnu.io.rxtx.SerialPorts", "/dev/ttyACM0"); I got rid of this
        CommPortIdentifier portId = null;
        Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
        //First, Find an instance of serial port as set in PORT_NAMES.
        while (portEnum.hasMoreElements()) {
            CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
            for (String portName : PORT_NAMES) {
                if (currPortId.getName().equals(portName)) {
                    portId = currPortId;
                    break;
                }
            }
        }        
        if (portId == null) {
            System.out.println("Could not find COM port.");
            return;
        }
        try {
            // open serial port, and use class name for the appName.
            serialPort = (SerialPort) portId.open(this.getClass().getName(),
                    TIME_OUT);
            // set port parameters
            serialPort.setSerialPortParams(DATA_RATE,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
            // open the streams
            input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
            output = serialPort.getOutputStream();
            // add event listeners
            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }
     /**
     * This should be called when you stop using the port.
     * This will prevent port locking on platforms like Linux.
     */
    public synchronized void close() {
        if (serialPort != null) {
            serialPort.removeEventListener();
            serialPort.close();
        }
    }
    /**
     * Handle an event on the serial port. Read the data and print it. In this case, it calls the mouseMove method.
     */
    public synchronized void serialEvent(SerialPortEvent oEvent) {
        if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            try {
                while (input.ready ()){
                String inputLine=input.readLine();
                mouseMove(inputLine);
                //System.out.println("********************");
            }
                //System.out.println(inputLine);
            } catch (Exception e) {
                System.err.println(e.toString());
            }
        }
        // Ignore all the other eventTypes, but you should consider the other ones.
    }
public static void main(String[] args) throws Exception     {
    
    
        
    
        Mouse main = new Mouse();
        main.initialize();
        Thread t=new Thread() {
            public void run() {
                //the following line will keep this app alive for 1000 seconds,
                //waiting for events to occur and responding to them (printing incoming messages to console).
                try {Thread.sleep(1000000);} catch (InterruptedException ie) {}
            }
        };
        t.start();
        System.out.println("Started");
    }
    
    // My method mouseMove, takes in a string containing the three data points and operates the mouse in turn
    public void mouseMove(String data) throws AWTException
    {   
        Audio knob = new Audio();
        
        
        System.out.println(data);
        int index1 = data.indexOf(" ", 0);
        int index2 = data.indexOf(" ", index1+1);
        int index3 = data.indexOf(" ", index2+1);
        int index4 = data.indexOf(" ", index3+1);
        int yCord = Integer.valueOf(data.substring(0, index1));
        int xCord = Integer.valueOf(data.substring(index1 + 1 , index2));
        int volVal1 = Integer.valueOf(data.substring(index2 + 1, index3));
        int scrVal = Integer.valueOf(data.substring(index3 + 1, index4));
        int button = Integer.valueOf(data.substring(index4 + 1));
        Robot robot = new Robot();
        
        int mouseY = MouseInfo.getPointerInfo().getLocation().y;
        int mouseX = MouseInfo.getPointerInfo().getLocation().x;
        
        if (button == 0)
    {
        if (buttonOld == 1)
        {
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.delay(10);
        }
    }
    else
    {
        if (buttonOld == 0)
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);            
    }
        
       
     
        //code addition for scrolling option using Scroll Lock
        
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        if((toolkit.getLockingKeyState(KeyEvent.VK_NUM_LOCK) == true) || (scrVal == 1))
            {System.out.println(yCord);
             //yCord=(int)(yCord*0.01);
             //System.out.println(yCord);
                if ((yCord)>510){
                    robot.mouseWheel(1);
                    robot.setAutoDelay(10000);
                    //robot.waitForIdle();
                    //try {Thread.sleep(100);} catch (InterruptedException ie) {}
                    //System.out.println(xCord);
                }
                else if ((yCord)<490){
                    robot.mouseWheel(-1);
                    //System.out.println(xCord);
                }
          
        }
        else{
        
                if (Math.abs(xCord - 500) > 5)
                    float x1 = (float)(xCord-500)/(float)(0.7/500);
                    float x2 = 10*(float)(Math.exp(x1)-1);
                    mouseX = mouseX + (int)((500 - x2));
                if (Math.abs(yCord - 500) > 5)
                    mouseY = mouseY - (int)((500 - yCord) * 0.021);
        
                robot.mouseMove(mouseX, mouseY);
                float volVal2 = (float)volVal1/1023;
                volVal2 = (float)(Math.round(volVal2*1000))/1000;
                System.out.println("Signal:" + volVal2);
                // float volVal3 = volVal2 + 0.01f;
                // float volVal4 = volVal2 - 0.01f;
                // if (volVal2 == volVal3){
                //  volVal2 = volVal2 - 0.01f;
                // }
                // if (volVal2 == volVal4){
                //  volVal2 = volVal2 + 0.01f;
                // }
                // System.out.println("Cleaned Signal:" + volVal2);
                knob.setMasterOutputVolume(volVal2);
                buttonOld = button;
                System.out.println(xCord + ":" + yCord + ":" + volVal1+" "+ ":" + button + ":" + mouseX + ":" + mouseY);
}
    return;
}
}

