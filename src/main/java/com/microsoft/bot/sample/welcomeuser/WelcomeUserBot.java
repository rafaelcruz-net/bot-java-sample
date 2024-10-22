// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.bot.sample.welcomeuser;

import com.codepoetics.protonpack.collectors.CompletableFutures;
import com.microsoft.bot.builder.ActivityHandler;
import com.microsoft.bot.builder.MessageFactory;
import com.microsoft.bot.builder.StatePropertyAccessor;
import com.microsoft.bot.builder.TurnContext;
import com.microsoft.bot.builder.UserState;
import com.microsoft.bot.schema.ActionTypes;
import com.microsoft.bot.schema.Activity;
import com.microsoft.bot.schema.CardAction;
import com.microsoft.bot.schema.CardImage;
import com.microsoft.bot.schema.ChannelAccount;
import com.microsoft.bot.schema.HeroCard;
import com.microsoft.bot.schema.ResourceResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * This class implements the functionality of the Bot.
 *
 * Represents a bot that processes incoming activities.
 * For each user interaction, an instance of this class is created and the
 * onTurn method is called.
 * This is a Transient lifetime service. Transient lifetime services are created
 * each time they're requested. For each Activity received, a new instance of
 * this
 * class is created. Objects that are expensive to construct, or have a lifetime
 * beyond the single turn, should be carefully managed.
 * For example, the "MemoryStorage" object and associated
 * StatePropertyAccessor{T} object are created with a singleton lifetime.
 * 
 * <p>
 * This is where application specific logic for interacting with the users would
 * be added. This class tracks the conversation state through a POJO saved in
 * {@link UserState} and demonstrates welcome messages and state.
 * </p>
 *
 * @see WelcomeUserState
 */
public class WelcomeUserBot extends ActivityHandler {
    // Messages sent to the user.
    private static final String INFO_MESSAGE = "Olá seja bem vindo ao bot do Ibmec, digite hello ou help";

    private final UserState userState;

    // Initializes a new instance of the "WelcomeUserBot" class.
    @Autowired
    public WelcomeUserBot(UserState withUserState) {
        userState = withUserState;
    }

    /**
     * Normal onTurn processing, with saving of state after each turn.
     *
     * @param turnContext The context object for this turn. Provides information
     *                    about the incoming activity, and other data needed to
     *                    process the activity.
     * @return A future task.
     */
    @Override
    public CompletableFuture<Void> onTurn(TurnContext turnContext) {
        return super.onTurn(turnContext)
                .thenCompose(saveResult -> userState.saveChanges(turnContext));
    }

    /**
     * Greet when users are added to the conversation.
     *
     * <p>
     * Note that all channels do not send the conversation update activity.
     * If you find that this bot works in the emulator, but does not in
     * another channel the reason is most likely that the channel does not
     * send this activity.
     * </p>
     *
     * @param membersAdded A list of all the members added to the conversation, as
     *                     described by the conversation update activity.
     * @param turnContext  The context object for this turn.
     * @return A future task.
     */
    @Override
    protected CompletableFuture<Void> onMembersAdded(
            List<ChannelAccount> membersAdded,
            TurnContext turnContext) {
        return membersAdded.stream()
                .filter(
                        member -> !StringUtils
                                .equals(member.getId(), turnContext.getActivity().getRecipient().getId()))
                .map(
                        channel -> turnContext
                                .sendActivities(
                                        MessageFactory.text(INFO_MESSAGE)))
                .collect(CompletableFutures.toFutureList())
                .thenApply(resourceResponses -> null);
    }

    /**
     * This will prompt for a user name, after which it will send info about the
     * conversation. After sending information, the cycle restarts.
     *
     * @param turnContext The context object for this turn.
     * @return A future task.
     */
    @Override
    protected CompletableFuture<Void> onMessageActivity(TurnContext turnContext) {
        // Get state data from UserState.
        StatePropertyAccessor<WelcomeUserState> stateAccessor = userState.createProperty("WelcomeUserState");
        CompletableFuture<WelcomeUserState> stateFuture = stateAccessor.get(turnContext, WelcomeUserState::new);

        return stateFuture.thenApply(thisUserState -> {
            // This example hardcodes specific utterances.
            // You should use LUIS or QnA for more advance language understanding.
            String text = turnContext.getActivity().getText().toLowerCase();
            switch (text) {
                case "hello":
                case "hi":
                    return turnContext.sendActivities(MessageFactory.text("You said " + text));
                case "intro":
                case "help":
                    return sendIntroCard(turnContext);

                default:
                    return turnContext.sendActivity("Diga alguma coisa para eu poder te ajudar ou digite help");

            }
        })
                // Save any state changes.
                .thenApply(response -> userState.saveChanges(turnContext))
                // make the return value happy.
                .thenApply(task -> null);
    }

    private CompletableFuture<ResourceResponse> sendIntroCard(TurnContext turnContext) {
        HeroCard card = new HeroCard();
        card.setTitle("Está com dúvidas?");
        card.setText(
                "Que tal aprender mais?");

        CardAction questionAction = new CardAction();
        questionAction.setType(ActionTypes.OPEN_URL);
        questionAction.setTitle("Ver Documentação do Bot Framework");
        questionAction.setText("Ver Documentação do Bot Framework");
        questionAction.setDisplayText("Ver Documentação do Bot Framework");
        questionAction.setValue("https://learn.microsoft.com/en-us/microsoftteams/platform/bots/bot-features");

        card.setButtons(Arrays.asList(questionAction));

        Activity response = MessageFactory.attachment(card.toAttachment());
        return turnContext.sendActivity(response);
    }
}
