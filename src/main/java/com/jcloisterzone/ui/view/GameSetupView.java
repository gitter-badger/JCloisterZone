package com.jcloisterzone.ui.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import com.google.common.eventbus.Subscribe;
import com.jcloisterzone.event.ClientListChangedEvent;
import com.jcloisterzone.event.GameStateChangeEvent;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.PlayerSlot;
import com.jcloisterzone.game.phase.CreateGamePhase;
import com.jcloisterzone.ui.Client;
import com.jcloisterzone.ui.GameController;
import com.jcloisterzone.ui.controls.chat.ChatPanel;
import com.jcloisterzone.ui.controls.chat.GameChatPanel;
import com.jcloisterzone.ui.panel.BackgroundPanel;
import com.jcloisterzone.ui.panel.ConnectedClientsPanel;
import com.jcloisterzone.ui.panel.CreateGamePanel;
import com.jcloisterzone.wsio.server.RemoteClient;

public class GameSetupView implements UiView {

	private final Client client;
	private final GameController gc;
	private final Game game;
	private final boolean mutableSlots;

	private ChatPanel chatPanel;
	private CreateGamePanel createGamePanel;
	private ConnectedClientsPanel connectedClientsPanel;

	public GameSetupView(Client client, GameController gc, boolean mutableSlots) {
		this.client = client;
		this.gc = gc;
		this.game = gc.getGame();
		this.mutableSlots = mutableSlots;
	}

	@Override
	public void show(Container pane) {
		Game game = gc.getGame();
    	CreateGamePhase phase = (CreateGamePhase)game.getPhase();

    	BackgroundPanel bg = new BackgroundPanel();
    	bg.setLayout(new BorderLayout());
    	pane.add(bg);

        showCreateGamePanel(bg, mutableSlots, phase.getPlayerSlots());

        //TODO remove legacy code
        client.setActivity(gc);
        client.setGame(game);
        //---
	}

	public void showCreateGamePanel(Container panel, boolean mutableSlots, PlayerSlot[] slots) {
        createGamePanel = new CreateGamePanel(client, gc, mutableSlots, slots);
        JPanel envelope = new BackgroundPanel();
        envelope.setLayout(new MigLayout("align 50% 50%", "[]", "[]")); //to have centered inner panel
        envelope.add(createGamePanel, "grow");

        panel.add(envelope, BorderLayout.CENTER);


        JPanel chatColumn = new JPanel();
        chatColumn.setOpaque(false);
        chatColumn.setLayout(new MigLayout("ins 0, gap 0 10", "[grow]", "[60px][grow]"));
        chatColumn.setPreferredSize(new Dimension(ChatPanel.CHAT_WIDTH, panel.getHeight()));
        panel.add(chatColumn, BorderLayout.WEST);

        chatColumn.add(connectedClientsPanel = new ConnectedClientsPanel(game.getName()), "cell 0 0, grow");

        JPanel chatBox = new JPanel();
        chatBox.setBackground(Color.WHITE);
        MigLayout chatBoxLayout = new MigLayout("", "[grow]", "[grow][]");
        chatBox.setLayout(chatBoxLayout);
        chatColumn.add(chatBox, "cell 0 1, grow");

        chatPanel = new GameChatPanel(client, game);
        chatPanel.registerSwingComponents(chatBox);
        chatBoxLayout.setComponentConstraints(chatPanel.getMessagesPane(), "cell 0 0, align 0% 100%");
        chatBoxLayout.setComponentConstraints(chatPanel.getInput(), "cell 0 1, growx");

        gc.register(createGamePanel);
        gc.register(chatPanel);
        gc.register(this);
    }

	@Override
	public void hide() {
		gc.unregister(createGamePanel);
        gc.unregister(chatPanel);
        gc.unregister(this);
	}

	@Subscribe
    public void started(GameStateChangeEvent ev) {
    	if (GameStateChangeEvent.GAME_START == ev.getType()) {
    		GameView view = new GameView(client, gc);
    		view.setChatPanel(chatPanel);
    		view.setSnapshot(ev.getSnapshot());
    		client.mountView(view);
    	}
    }

	@Subscribe
    public void clientListChanged(ClientListChangedEvent ev) {
    	RemoteClient[] clients = ev.getClients();
        connectedClientsPanel.updateClients(clients);
    }

}