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
    
    
    public class Mouse implements SerialPortEventListener {
        SerialPort serialPort;
            /** The port we're normally going to use. */
        private static final String PORT_NAMES[] = {"/dev/ttyUSB0"};
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
            int index1 = data.indexOf(" ", 0);
            int index2 = data.indexOf(" ", index1+1);
            int yCord = Integer.valueOf(data.substring(0, index1));
            int xCord = Integer.valueOf(data.substring(index1 + 1 , index2));
            int button = Integer.valueOf(data.substring(index2 + 1));
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
            if(toolkit.getLockingKeyState(KeyEvent.VK_NUM_LOCK) == true)
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
                        mouseX = mouseX + (int)((500 - xCord) * 0.011);
                    if (Math.abs(yCord - 500) > 5)
                        mouseY = mouseY - (int)((500 - yCord) * 0.011);
            
                    robot.mouseMove(mouseX, mouseY);
            
                    buttonOld = button;
                    System.out.println(xCord + ":" + yCord + ":" + button + ":" + mouseX + ":" + mouseY);
    }
        return;
    }
}

