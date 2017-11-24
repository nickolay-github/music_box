package book_exs;
import com.sun.xml.internal.messaging.saaj.soap.JpegDataContentHandler;

import java.awt.*;
import javax.swing.*;
import javax.sound.midi.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.io.*;
import java.util.*;
import java.net.*;
import java.awt.event.*;

public class BeatBox {

    JPanel mainPanel;
    JFrame theFrame;
    JList incomingList;
    JTextField userMessage;
    ArrayList<JCheckBox> checkboxList;
    int nextNum;
    Vector<String> listVector = new Vector<String>();
    String userName;
    ObjectOutputStream out;
    ObjectInputStream in;
    HashMap<String, boolean[]> otherSeqsMap = new HashMap<String, boolean[]>();

    Sequencer sequencer;
    Sequence sequence;
    Sequence mySequence = null;
    Track track;


    String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat",
    "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand Clap",
    "High Tom", "Hi Bong", "Maracas", "Whistle", "Low Conga",
    "Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo",
    "Open Hi Conga"};
    int[] instruments = {35, 42,46,38,49,39,50,60,70,72,64,56,58,47,67,63};

    public static void main(String[] args) {

        new BeatBox().startUp("gg allin");
    }
    public void startUp(String name) {
        userName = name;
        try {
            Socket sock = new Socket("127.0.0.1", 4242);
            out = new ObjectOutputStream(sock.getOutputStream());
            in = new ObjectInputStream(sock.getInputStream());
            Thread remote = new Thread(new RemoteReader());
            remote.start();
        } catch (Exception ex) {
            System.out.println("No connect!");
        }
        setUpMidi();
        buildGUI();
    }
    public void buildGUI() {
        theFrame = new JFrame("Cyber BeatBox");
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10,10,10, 10));

        checkboxList = new ArrayList<JCheckBox>();

        Box buttonBox = new Box(BoxLayout.Y_AXIS);
        JButton start = new JButton("Start");
        start.addActionListener(new MyStartListener());
        buttonBox.add(start);
        buttonBox.add(Box.createRigidArea(new Dimension(5,0)));

        JButton stop = new JButton("Stop");
        stop.addActionListener(new MyStopListener());
        buttonBox.add(stop);
        buttonBox.add(Box.createRigidArea(new Dimension(5,0)));

        JButton upTempo = new JButton("Tempo Up");
        upTempo.addActionListener(new MyUpTempoListener());
        buttonBox.add(upTempo);
        buttonBox.add(Box.createRigidArea(new Dimension(5,0)));

        JButton downTempo = new JButton("Tempo Down");
        downTempo.addActionListener(new MyDownTempoListener());
        buttonBox.add(downTempo);

        JButton restore = new JButton("Restore");
        restore.addActionListener(new MyRestoreListener());
        buttonBox.add(restore);

        JButton serializIt = new JButton("Serialize It");
        serializIt.addActionListener(new SerializeListener());
        buttonBox.add(serializIt);

        JButton clear = new JButton("Clear");
        clear.addActionListener(new MyClearListener());
        buttonBox.add(clear);

        JButton sendIt = new JButton("sendIt");
        sendIt.addActionListener(new MySendListener());
        buttonBox.add(sendIt);

        JTextField userMessage = new JTextField();
        buttonBox.add(userMessage);

        incomingList = new JList();
        incomingList.addListSelectionListener(new MyListSelectionListener());
        incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane theList = new JScrollPane(incomingList);
        buttonBox.add(theList);
        incomingList.setListData(listVector);

        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for (int i=0; i < 16; i++) {
            nameBox.add(new Label(instrumentNames[i]));
        }

        background.add(BorderLayout.EAST, buttonBox);
        background.add(BorderLayout.WEST, nameBox);

        theFrame.getContentPane().add(background);

        GridLayout grid = new GridLayout(16,16);
        grid.setVgap(1);
        grid.setHgap(2);
        mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);

        for (int i=0; i < 256; i++) {
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkboxList.add(c);
            mainPanel.add(c);
        }

        setUpMidi();

        theFrame.setBounds(50, 50,300,300);
        theFrame.pack();
        theFrame.setVisible(true);
    }

    public void setUpMidi() {
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ, 4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);
        } catch (Exception e) {e.printStackTrace();}
    }

    public void buildTrackAndStart() {

        ArrayList<Integer> trackList = null;

        sequence.deleteTrack(track);
        track = sequence.createTrack();

        for (int i=0; i < 16; i++) {
            trackList = new ArrayList<Integer>();

            for (int j=0; j < 16; j++) {
                JCheckBox jc = (JCheckBox) checkboxList.get(j + (16*i));
                if (jc.isSelected()) {
                    int key = instruments[i];
                    trackList.add(new Integer(key));
                } else {
                    trackList.add(null);
                }
            }
            makeTracks(trackList);
            track.add(makeEvent(176, 1, 127, 0, 16));
        }
        track.add(makeEvent(192, 9, 1, 0, 15));
        try {
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
            sequencer.setTempoInBPM(120);
        } catch (Exception e) {e.printStackTrace();}
    }

    public class MyStartListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            buildTrackAndStart();
        }
    }

    public class MyStopListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            sequencer.stop();
        }
    }

    public class MyUpTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * 1.03));
        }
    }

    public class MyClearListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            for (int i=0; i < 256; i++) {
                JCheckBox check = (JCheckBox) checkboxList.get(i);
                if (check.isSelected()) {
                    check.setSelected(false);
                }
            }
            sequencer.stop();
        }
    }

    public class MyDownTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * 0.97));
        }
    }

    public class SerializeListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            boolean[] checkboxState = new boolean[256];

            for (int i=0; i < 256; i++) {
                JCheckBox check = (JCheckBox) checkboxList.get(i);
                if (check.isSelected()) {
                    checkboxState[i] = true;
                }
            }

            try {
                JFileChooser fileSave = new JFileChooser();
                fileSave.showOpenDialog(theFrame);
                FileOutputStream fileStream = new FileOutputStream(fileSave.getSelectedFile());
                ObjectOutputStream os = new ObjectOutputStream(fileStream);
                os.writeObject(checkboxState);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public class MyRestoreListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            boolean[] checkboxState = null;

            try {
                JFileChooser fileOpen = new JFileChooser();
                fileOpen.showOpenDialog(theFrame);
                FileInputStream fileIn = new FileInputStream(fileOpen.getSelectedFile());
                ObjectInputStream is = new ObjectInputStream(fileIn);
                checkboxState = (boolean[]) is.readObject();

            } catch (Exception ex) { ex.printStackTrace();}

            for (int i=0; i < 256; i++) {
                JCheckBox check = (JCheckBox) checkboxList.get(i);
                if (checkboxState[i]) {
                    check.setSelected(true);
                } else {
                    check.setSelected(false);
                }
            }

            sequencer.stop();
            buildTrackAndStart();
        }
    }

    public class MySendListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            boolean[] checkboxState = new boolean[256];
            for (int i = 0; i < 256; i++) {
                JCheckBox check = (JCheckBox) checkboxList.get(i);
                if (check.isSelected()) {
                    checkboxState[i] = true;
                }
            }
            String messageToSend = null;
            try {
                out.writeObject(userName + nextNum++ + ": " + userMessage.getText());
                out.writeObject(checkboxState);
            } catch (Exception e) {
                System.out.println("Sorry dude. Could not send it to the server.");
            }
            userMessage.setText("");
        }
    }

    public class MyListSelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent le) {
            if (!le.getValueIsAdjusting()) {
                String selected = (String) incomingList.getSelectedValue();
                if (selected != null) {
                    //Загрузка и отображение выбранной из чата  последовательности
                    boolean[] selectedState = (boolean[]) otherSeqsMap.get(selected);
                    changeSequence(selectedState);
                    sequencer.stop();
                    buildTrackAndStart();
                }
            }
        }
    }
    public class RemoteReader implements Runnable {
        boolean[] checkboxState = null;
        String nameToShow = null;
        Object obj = null;
        public void run() {
            try {
                while((obj=in.readObject()) != null) {
                    System.out.println("got an object from server");
                    System.out.println(obj.getClass());
                    String nameToShow = (String) obj;
                    checkboxState = (boolean[]) in.readObject();
                    otherSeqsMap.put(nameToShow, checkboxState);
                    listVector.add(nameToShow);
                    incomingList.setListData(listVector);
                    }
            } catch (Exception ex) {ex.printStackTrace();}
        }
    }

    public class MyPlayMineListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            if (mySequence != null) {
                sequence = mySequence;
            }
        }
    }

    public void changeSequence(boolean[] checkboxState) {
        for (int i=0; i<256; i++) {
            JCheckBox check = (JCheckBox) checkboxList.get(i);
            if (checkboxState[i]) {
                check.setSelected(true);
            } else {
                check.setSelected(false);
            }
        }
    }



    public void makeTracks(ArrayList list) {
        Iterator it = list.iterator();
        for (int i=0; i < 16; i++) {
            Integer num = (Integer) it.next();
            if (num != null) {
                int numKey = num.intValue();
                track.add(makeEvent(144,9,numKey,100, i));
                track.add(makeEvent(128,9,numKey,100,i+1));
            }
        }
    }

    public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
        MidiEvent event = null;
        try {
            ShortMessage a = new ShortMessage();
            a.setMessage(comd,chan,one,two);
            event = new MidiEvent(a, tick);
        } catch (Exception e) {e.printStackTrace(); }
        return event;
    }



}
