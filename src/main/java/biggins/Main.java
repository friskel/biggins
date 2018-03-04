package biggins;

import info.bitrich.xchangestream.bitmex.BitmexStreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.marketdata.Trade;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;

public class Main {

    private static JFrame frame;
    private static JPanel mainPanel;

    private static JLabel text;

    private static int minimumTrade = 10000;
    private static int maxDisplay = 20;

    private static boolean hideTopSelected = false;
    private static boolean alwaysOnTopSelected = false;

    private static Trade bunch = null;
    private static boolean addedThisOne = false;

    private static boolean firstStart = true;

    private static ArrayList<Trade> trades = new ArrayList<>();

    public static void main(String[] args) {

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                start(true, new Point());
                startStream();
            }


        });

    }

    private static void startStream() {

        StreamingExchange exchange = StreamingExchangeFactory.INSTANCE.createExchange(BitmexStreamingExchange.class.getName());
        exchange.connect().blockingAwait();


        exchange.getStreamingMarketDataService()
                .getTrades(new CurrencyPair(Currency.XBT, Currency.USD))
                .subscribe(trade -> {
                    System.out.print(trade.getOriginalAmount().toString() + trade.getType());

                    //if no bunch start new one
                    if (bunch == null) {
                        addedThisOne = false;
                        System.out.println(" - bunch null, adding this trade");
                        bunch = new Trade(trade.getType(), trade.getOriginalAmount(), trade.getCurrencyPair(), trade.getPrice(), trade.getTimestamp(), trade.getId());

                        //if bunch over min, send it and clear
                        if (bunch.getOriginalAmount().doubleValue() > minimumTrade) {
                            System.out.println(" ++++ over min, sending and clearing fresh trade.  amt: " + bunch.getOriginalAmount());
                            addOne(bunch);
                            bunch = null;
                        }

                    }
                    //if there is a bunch, and this trade has same timestamp/type, add to bunch
                    else if (trade.getTimestamp().getTime() == bunch.getTimestamp().getTime() && trade.getType() == bunch.getType()) {

                        bunch = new Trade(bunch.getType(), new BigDecimal(bunch.getOriginalAmount().doubleValue() + trade.getOriginalAmount().doubleValue()), bunch.getCurrencyPair(), bunch.getPrice(), bunch.getTimestamp(), bunch.getId());
                        System.out.print(" + add to bunch now at: " + bunch.getOriginalAmount() + ". ");

                        if (bunch.getOriginalAmount().doubleValue() >= minimumTrade) {
                            System.out.println("just added to bunch, we are over min trade");
                            if (!addedThisOne) {
                                System.out.println("haven't added this one, add it");
                                addOne(bunch);
                                addedThisOne = true;
                            } else {
                                System.out.println("panel already up, just update");
                                updateOne(bunch);
                            }

                        } else {
//                            System.out.println("just added to bunch, we are not over min trade, not displaying panel");
                        }


                    }
                    //if there is a bunch but old timestamp or new type, new bunch with this trade
                    else {

                        addedThisOne = false;

                        //if bunch over min, send it and clear
                        if (bunch.getOriginalAmount().doubleValue() > minimumTrade) {
                            System.out.println("new trade has diff time/type, over min, nulling current bunch");
//                            addOne(bunch);
                            bunch = null;
                        }


                        bunch = new Trade(trade.getType(), trade.getOriginalAmount(), trade.getCurrencyPair(), trade.getPrice(), trade.getTimestamp(), trade.getId());
//                        System.out.println(" - time/type off, new bunch at " + bunch.getOriginalAmount() + " time: " + bunch.getTimestamp().getTime());

                        //if bunch over min, send it and clear
                        if (bunch.getOriginalAmount().doubleValue() > minimumTrade) {
                            System.out.println(" ++++++ over min, sending and clearing " + bunch.getOriginalAmount());
                            addOne(bunch);
                            bunch = null;
                        }
                    }

//                    addOne(trade);
                }, throwable -> {
                    System.out.println("Error in subscribing trades." + throwable);
                });


    }

    private static void updateOne(Trade trade) {

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {

                JPanel tradepan = (JPanel) mainPanel.getComponent(0);

                JLabel text = (JLabel) tradepan.getComponent(0);

                text.setText(trade.getOriginalAmount().toString() + "updated");

                frame.revalidate();

            }

        });

    }

    private static void addOne(Trade trade) {

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {

                JPanel tradepan = new JPanel();
                tradepan.setMinimumSize(new Dimension(200, 100));
                if (trade.getType().equals(Order.OrderType.BID)) {
                    tradepan.setBackground(Color.GREEN);
                } else {
                    tradepan.setBackground(Color.RED);
                }

                JLabel text1 = new JLabel(trade.getOriginalAmount().toString() + " @ " + trade.getPrice());
                text1.setVerticalAlignment(SwingConstants.CENTER);
                text1.setHorizontalAlignment(SwingConstants.CENTER);

                tradepan.add(text1);

                mainPanel.add(tradepan, 0);

                if (mainPanel.getComponentCount() >= maxDisplay) {
                    mainPanel.remove(mainPanel.getComponentCount() - 1);
                }

//                text.setText(trade.getType() + ": " + trade.getOriginalAmount().toString() + " @ " + trade.getPrice() + "\n" + text.getText());
                frame.revalidate();

            }

        });

    }

    private static void start(boolean titleBar, Point position) {


        frame = new JFrame("Biggins");
        frame.setUndecorated(!titleBar);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setMinimumSize(new Dimension(150, 400));
        frame.setLocation(position);
        frame.setVisible(true);
        mainPanel = new JPanel(new GridLayout(0, 1));
        mainPanel.setMinimumSize(new Dimension(150, 1000));


        for (int i = 0; i < maxDisplay; i++) {

            JPanel pan = new JPanel();
            pan.setMinimumSize(new Dimension(200, 100));
            pan.setBackground(Color.green);
            JLabel text = new JLabel("hi");
            pan.add(text);
            mainPanel.add(pan, 0);

        }
        frame.setContentPane(mainPanel);


        mainPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                getSettings();
            }
        });

    }

    private static void getSettings() {

        JPanel panel = new JPanel(new GridLayout(0, 1));

        //combo box for ppair picker
        String[] items = {"XBTUSD", "XBTH18", "NEOH18", "list", "thepairs"};
        JComboBox<String> combo = new JComboBox<>(items);
        panel.add(combo);

        //minimum trade
        panel.add(new JLabel("Minimum trade:"));
        JTextField field1 = new JTextField("10000");
        panel.add(field1);

        //hide frame
        JRadioButton hideTopRadio = new JRadioButton("Hide window frame");
        hideTopRadio.setSelected(hideTopSelected);
        panel.add(hideTopRadio);

        //always on top
        JRadioButton alwaysOnTop = new JRadioButton("Always on top");
        alwaysOnTop.setSelected(alwaysOnTopSelected);
        panel.add(alwaysOnTop);

        /////////////////////do it
        int result = JOptionPane.showConfirmDialog(null, panel, "Settings",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            System.out.println("settings OK button");

            //commit settings
            minimumTrade = Integer.valueOf(field1.getText());


            if (hideTopRadio.isSelected() && !hideTopSelected) {
                Point framePosition = frame.getLocationOnScreen();
                frame.invalidate();
                frame.dispose();
                start(false, framePosition);
                hideTopSelected = true;
            } else if (!hideTopRadio.isSelected() && hideTopSelected) {
                Point framePosition = frame.getLocationOnScreen();
                frame.invalidate();
                frame.dispose();
                start(true, framePosition);
                hideTopSelected = false;
            }

            if (alwaysOnTop.isSelected() && !alwaysOnTopSelected) {
                frame.setAlwaysOnTop(true);
                alwaysOnTopSelected = true;
            } else if (!alwaysOnTop.isSelected() && alwaysOnTopSelected) {
                frame.setAlwaysOnTop(false);
                alwaysOnTopSelected = false;
            }

        }

    }


}
