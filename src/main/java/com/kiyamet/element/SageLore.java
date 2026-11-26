package com.kiyamet.element;

/** 2.1: every sage has a name and tells the story of their element. */
public final class SageLore {
    public static String name(Element e) {
        return switch (e) {
            case FLAME -> "Ignar the Ember-Keeper"; case LIGHTNING -> "Voltra Skyborn"; case EARTH -> "Old Brannoc Stonebeard"; case SHADOW -> "Nyxa the Veiled";
            case SMOKE -> "Ashwin Greycloak"; case SPIRIT -> "Elowen Soulwhisper"; case ELECTRIC -> "Tesmir Coilhand"; case THUNDER -> "Thorvald Stormcaller";
            case ICE -> "Frida Rimeheart"; case FIRE -> "Kaelen Pyreborn"; case WATER -> "Marisol Tidesinger"; case AIR -> "Aeris the Unbound";
            case WIND -> "Zephyrin Gale-Walker"; case METAL -> "Ferros Ironmind";
        };
    }

    public static String greeting(Element e) {
        return switch (e) {
            case FLAME -> "Sit by the fire, stranger. Fire remembers every face it has warmed.";
            case LIGHTNING -> "You came on foot? The sky would have carried you faster. Welcome.";
            case EARTH -> "Hmph. Another one walking on my mountains. Well - sit, sit.";
            case SHADOW -> "I saw you long before you saw my tent. Don't be afraid of the dark. Be afraid of what hides in the light.";
            case SMOKE -> "Mind the smoke, it stings the eyes. It also hides the dead from us. Mostly.";
            case SPIRIT -> "Your soul is loud, traveller. Quieter, please - the dead are listening.";
            case ELECTRIC -> "Careful with the coil! Ah. A visitor. When did I last have one of those?";
            case THUNDER -> "HA! A warrior! The storm told me someone was coming.";
            case ICE -> "Close the tent, the cold gets in. ...No. Leave it open. I like the cold.";
            case FIRE -> "You smell of walkers and ash. Good. Then you have lived through something.";
            case WATER -> "The river brought you to me. Rivers never lie about where they go.";
            case AIR -> "Look up, not down. Everyone looks down these days.";
            case WIND -> "The wind changed this morning. It always does before someone important arrives.";
            case METAL -> "Your blade is notched and your boots are worn. Sit. Let me look at you.";
        };
    }

    /** the element's story, told one part each time you talk */
    public static String[] story(Element e) {
        return switch (e) {
            case FLAME -> new String[]{
                    "Before the cities, before the first wall, there was one flame. Our people carried it from cave to cave for a thousand years.",
                    "The Flame Temple was built around that first fire. It has never gone out - not even when the dead came.",
                    "The Flame Lord was a keeper like me, once. He swore to guard the fire forever. Forever is a long time. It burned him hollow.",
                    "The Flame Sword was forged from the same fire. Whoever holds it can breathe flame like a dragon of the old songs.",
                    "When the plague came, the flame was the only thing the walkers feared. Remember that, when your nights are long."};
            case LIGHTNING -> new String[]{
                    "My grandmother said lightning is the sky choosing someone. She was struck three times and lived to be a hundred.",
                    "The Lightning Temple stands where the first bolt hit the earth. The stone there still hums.",
                    "The Lightning Lord does not walk - he strikes. Wherever you stand, he can be there in a flash.",
                    "The mace remembers every storm. Raise it, and the sky answers you.",
                    "The dead do not understand the sky. That is why they flinch when it thunders."};
            case EARTH -> new String[]{
                    "Mountains are patient. I learned it from them. Took me sixty years, mind you.",
                    "The Earth Temple was not built. It was dug - down and down, until the diggers found the heart of the ground.",
                    "The Earth Lord is the oldest of the fourteen. Old as the roots. Slow, they say. They are wrong.",
                    "The Earth Axe does not cut. It breaks. Stone, walls, the dead - it makes no difference to it.",
                    "When the world ends - and it did, didn't it - the ground is still here. Everything comes back to the earth."};
            case SHADOW -> new String[]{
                    "Shadow is not darkness. Shadow is what light makes when something stands in its way.",
                    "The Shadow Temple has no windows. Its builders were blind, and they did not need them.",
                    "The Shadow Lord walks behind you. Always behind you. Do not turn around too fast.",
                    "The katana was folded in a room without light. Holding it, you become what the eye forgets.",
                    "The dead hunt by sight and sound. Shadow takes away the first. Silence is your job."};
            case SMOKE -> new String[]{
                    "Every fire leaves smoke. Everyone forgets the smoke. That is its power.",
                    "The Smoke Temple was built by people who hid from war. Then they learned the smoke could hunt too.",
                    "The Smoke Lord fights blind and breathes poison. Hold your breath, and hit hard.",
                    "The sai were made to catch blades. Now they catch the breath of anything near you.",
                    "I've smoked this pipe since before the plague. Some habits even the end of the world can't break."};
            case SPIRIT -> new String[]{
                    "When we die, something leaves. The walkers prove it - their bodies stayed, but nothing came back inside.",
                    "The Spirit Temple was built where the dying went to say goodbye. Thousands of souls remember the way.",
                    "The Spirit Lord gathers lost souls like coins. He has a great many.",
                    "With the Spirit Spear, the body becomes as light as the soul. You will fly faster than any bird.",
                    "I hear them, you know. The ones the walkers used to be. They are not angry. Only tired."};
            case ELECTRIC -> new String[]{
                    "Electricity is just lightning that learned to behave. Mostly.",
                    "The Electric Temple was a power station once, older than any city. The ancients ran the world from it.",
                    "The Electric Lord is half machine now. I'd love to take him apart. Carefully.",
                    "The scythe throws lightning that bursts. Don't use it indoors. Trust me.",
                    "When the grid died, the walkers came out at night. Light is a wall, friend. Never forget it."};
            case THUNDER -> new String[]{
                    "Lightning is the flash. Thunder is what it says. My people listened.",
                    "The Thunder Temple sits on the stormiest mountain in the world. The rain there has not stopped in a hundred years.",
                    "The Thunder Lord is fast. So fast that time itself seems to slow around him.",
                    "With the hammer, you move like the storm and the world crawls. A gift of the old gods - or a curse.",
                    "I fought walkers with my bare hands when the plague came. Ha! A hammer is better."};
            case ICE -> new String[]{
                    "Ice keeps things. Fish, meat, memories. My father is still frozen in the lake where he fell. I visit him.",
                    "The Ice Temple was carved inside a glacier. It has not melted since the first winter.",
                    "The Ice Lord can stop a heart with a look. And time, they say. I believe it.",
                    "With the nunchaku, the whole world freezes. Only you move. It is very quiet.",
                    "The cold slows the dead. Winter is the only season that is on our side."};
            case FIRE -> new String[]{
                    "Flame is the candle. Fire is the forest burning. Different things, child.",
                    "The Fire Temple stands on black stone, where a mountain once broke open and bled.",
                    "The Fire Lord throws suns. Small ones. They still hurt.",
                    "The club was a torch-bearer's weapon. Now it hurls fire like a war machine.",
                    "We burned the first walkers. It was the only thing that worked. It is still the thing that works."};
            case WATER -> new String[]{
                    "Water always wins. It is soft, so it never breaks. It waits, so it always arrives.",
                    "The Water Temple was built for the sea gods. The sea took the city around it, but left the temple standing.",
                    "The Water Lord pulls you in like a current. Keep your feet, or the deep takes you.",
                    "The shuriken spins like a whirlpool. With it, you leap like a wave and swim like a dolphin.",
                    "Walkers cannot swim. When all else fails - find water."};
            case AIR -> new String[]{
                    "Air is everywhere and no one sees it. Like love. Like grief.",
                    "The Air Temple was built by people who wanted to be closer to the sky. They got their wish.",
                    "The Air Lord does not touch the ground. He has not, for a very long time.",
                    "The kunai is light as a feather - and so are you, when you hold it. Rise.",
                    "From above, the walkers look like ants. Small. Slow. Remember that view."};
            case WIND -> new String[]{
                    "Air is still. Wind moves. I prefer things that move.",
                    "The Wind Temple's halls are full of moving air. The builders say they heard voices in it.",
                    "The Wind Lord pulls and pushes. Never fight him in the open.",
                    "The triple scythe makes time race like the wind. Days pass in hours.",
                    "Follow the wind and you'll never stay in one place long enough for the dead to find you."};
            case METAL -> new String[]{
                    "Metal is earth that has been through fire. It remembers both.",
                    "The Metal Temple was a forge for kings. The anvils there are older than any crown.",
                    "The Metal Lord cannot be cut. Can't be crushed either. You'll need patience, and something heavier.",
                    "With the claws, nothing can hurt you. Nothing at all. Do not let it make you careless.",
                    "I lost this arm to a walker. Built a better one. Never let them take something from you without taking something back."};
        };
    }

    public static String companion(Element e) {
        return switch (e) {
            case FLAME -> "My fire has waited long enough. I'll walk with you."; case LIGHTNING -> "Finally! Somewhere to go!";
            case EARTH -> "My knees complain, but my axe doesn't. Lead on."; case SHADOW -> "I will be behind you. Always.";
            case SMOKE -> "I'll bring the pipe."; case SPIRIT -> "The souls say you are kind. I'll come.";
            case ELECTRIC -> "Can I bring the coil? I'm bringing the coil."; case THUNDER -> "HA! At last, a real fight!";
            case ICE -> "I'll come. Keep the fires small."; case FIRE -> "Let's burn them all.";
            case WATER -> "The river goes your way. So do I."; case AIR -> "Up we go.";
            case WIND -> "The wind has turned. It points at you."; case METAL -> "My arm is yours.";
        };
    }

    private SageLore() {}
}
