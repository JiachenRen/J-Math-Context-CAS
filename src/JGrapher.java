import jmc.*;
import jui.*;
import jui.bundles.ColorSelector;
import jui.bundles.ValueSelector;
import processing.core.PApplet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Jiachen on 22/05/2017.
 * JGrapher, created May 22nd. Breakthrough.
 */
public class JGrapher extends PApplet {
    private Graph graph;
    private HBox parent;
    private HBox functionInputWrapper;
    private VBox graphWrapper;
    private boolean casEnabled = false;

    public static void main(String args[]) {
        System.out.println("Welcome to JGrapher, an extensive graphing/calculation system using original CAS and UI library. \n" +
                "Copyright (c) 2018, Jiachen Ren. \n" +
                "MIT license applies. \n");
        String sketch = Thread.currentThread().getStackTrace()[1].getClassName();
        Thread proc = new Thread(() -> PApplet.main(sketch));
        proc.start();
    }

    public void settings() {
        size(1100, 780, FX2D);
    }

    private static void enableCoreDump() {
        try {
            Runtime.getRuntime().exec("ulimit -c unlimited");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Process process = Runtime.getRuntime().exec("ulimit -c -l");
            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String response = "", temp;
            while ((temp = input.readLine()) != null) response += temp;
            if (response.contains("(blocks, -c) 0")) {
                System.err.println("# Fatal Error: Core Dump Failed --> Resurrecting... Failed");
                //enableCoreDump();
                //exit();
            } else System.out.println("# Core Dump Successfully Enabled");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void setup() {
        JNode.init(this);
        if (!JNode.OS.contains("windows")) JGrapher.enableCoreDump();

        JNode.DISPLAY_CONTOUR = true;
        JNode.CONTOUR_THICKNESS = 0.1f;
//        JNode.CONTOUR_COLOR = color(0, 0, 100);
        JNode.BACKGROUND_COLOR = color(255, 255, 255, 50);
        JNode.TEXT_COLOR = color(255, 255, 255, 255);
        JNode.MOUSE_OVER_TEXT_COLOR = color(255, 255, 255, 255);
        JNode.MOUSE_PRESSED_TEXT_COLOR = color(255, 255, 255, 255);
        JNode.MOUSE_OVER_BACKGROUND_COLOR = color(255, 255, 255, 100);
        JNode.MOUSE_PRESSED_BACKGROUND_COLOR = color(255, 255, 255, 75);
        JNode.ROUNDED = false;

        parent = new HBox(0, 0, width, height);
        parent.setCollapseInvisible(true)
                .setId("parent")
                .setMargins(1, 0)
                .matchWindowDimension(true);

        VBox std = new VBox(0.1f, 1.0f);
        std.setMarginX(0);
        parent.add(std);

        graphWrapper = new VBox();
        graphWrapper.setId("graphWrapper").setCollapseInvisible(true);
        parent.add(graphWrapper);

        Displayable modelLabel = new Label()
                .setTextColor(0, 0, 0, 255)
                .setAlign(CENTER)
                .setContourVisible(false)
                .setBackgroundColor(255, 255, 255, 200);

        TextInput modelInput = (TextInput) new TextInput()
                .setCursorColor(255)
                .setCursorThickness(1);
//                .setTextColor(230)
//                .setBackgroundColor(50);

        Button modelButton = new Button();

//        graphWrapper.add(new Label("Grapher Version 1.0 By Jiachen Ren").inheritOutlook(modelLabel));

        graph = (Graph) new Graph().setRelative(true).setId("graph");
        graph.setTextColor(modelLabel.backgroundColor);
        graph.setBackgroundColor(0, 0, 0, 100);
        graphWrapper.add(graph);


        functionInputWrapper = new HBox(1.0f, 0.035f);
        functionInputWrapper.setMargins(0, 0).setId("#functionInputWrapper");
        graphWrapper.add(functionInputWrapper);

        TextInput funcNameTextInput = new TextInput();
        funcNameTextInput.inheritOutlook(modelInput)
                .inheritMode(modelInput);
        Runnable updateAdvPanel = () -> {
            String name = funcNameTextInput.getContent();
            Function func = graph.getFunction(name);
            if (func == null) return;
            ValueSelector strokeWeight = (ValueSelector) JNode.get("#0").get(0);
            strokeWeight.setValue(func.getStrokeWeight());

            Button showAsymptotes = (Button) JNode.get("#1").get(0);
            showAsymptotes.setContent(func.isAsymptoteVisible() ? "Hide Asymptote" : "Show Asymptote");

            Button showTangentLine = (Button) JNode.get("#2").get(0);
            showTangentLine.setContent(func.isTangentLineVisible() ? "Hide f'(x)" : "Show f'(x)");

            Button visibility = (Button) JNode.get("#3").get(0);
            visibility.setContent(func.isVisible() ? "Visible" : "Invisible");

            Button continuous = (Button) JNode.get("#4").get(0);
            continuous.setContent(func.getStyle().equals(Function.Style.CONTINUOUS) ? "Continuous" : "Discrete");

            Button dynamic = (Button) JNode.get("#5").get(0);
            dynamic.setContent(func.isDynamic() ? "Dynamic" : "Static");

            ColorSelector functionColor = (ColorSelector) JNode.get("#6").get(0);
            functionColor.setLinkedColorVars(name);
            functionColor.setColor(name, func.getColor());
            functionColor.link(name, () -> graph.getFunction(name).setColor(functionColor.getColorRGBA(name)));
            functionColor.setAsSingleVarMode(); //in lambda, the original function became immutable, thus it needs to be reacquired.

            Button extensionOn = (Button) JNode.get("#7").get(0);
            extensionOn.setContent(func.isAutoAsymptoteExtension() ? "On" : "Off");

            Switch tracingOn = (Switch) JNode.get("#8").get(0);
            tracingOn.setState(func.tracingEnabled());

            Switch match = (Switch) JNode.get("#9").get(0);
            match.setState(func.isMatchAuxiliaryLinesColor());

        };
        TextInput funcTextInput = new TextInput().inheritOutlook(modelInput)
                .inheritMode(modelInput);
        funcNameTextInput.setContent("f(x)=").setAlign(CENTER).setId("funcNameTextInput").setRelativeW(0.13f).addEventListener(Event.CONTENT_CHANGED, () -> {
            String name = funcNameTextInput.getContent();
            Function func = graph.getFunction(name);
            if (func == null) return;
            updateAdvPanel.run();
            funcTextInput.setContent(((InterpretedFunction) func).getOperable().toString());
        });
        functionInputWrapper.add(funcNameTextInput);


        //Dynamic function interpretation
        funcTextInput.onSubmit(() -> {
            try {
                Operable original = Function.interpret(funcTextInput.getStaticContent()).getOperable();
                if (casEnabled) {
                    if (original instanceof BinaryOperation)
                        original = ((BinaryOperation) original).simplify();
                    else if (original instanceof UnaryOperation)
                        original = ((UnaryOperation) original).simplify();
                }
                funcTextInput.setContent(original.toString());
            } catch (RuntimeException e) {
                System.out.println((char) 27 + "[1;34m" + "simplification failed -> incomplete input" + (char) 27 + "[0m");
            }
        }).setDefaultContent(" type in your function  here").setId("f(x)");
        funcTextInput.onKeyTyped(() -> {
            try {
                graph.override(funcNameTextInput.getContent(), Function.interpret(funcTextInput.getContent()));
                updateAdvPanel.run();
            } catch (RuntimeException e) {
                System.out.println((char) 27 + "[1;31m" + "interpretation incomplete -> pending..." + (char) 27 + "[0m");
            }
        });
        functionInputWrapper.add(funcTextInput);

        VBox adv = new VBox(0.1f, 1f);
        adv.setMarginX(0);
        adv.setVisible(false);
        parent.add(adv);


        std.add(new Label().setContent("Window").inheritOutlook(modelLabel));

        Runnable window = () -> {
            @SuppressWarnings("unchecked") ArrayList<TextInput> displayables = (ArrayList<TextInput>) JNode.get("$window");
            graph.setWindow(displayables.get(0).getFloatValue(), displayables.get(1).getFloatValue(), displayables.get(2).getFloatValue(), displayables.get(3).getFloatValue());
        };

        Label windowModelLabel = new Label();
        windowModelLabel.setContent("Max X")
                .inheritOutlook(modelLabel)
                .setRelativeW(0.3f);

        TextInput windowModelTextInput = new TextInput();
        windowModelTextInput.inheritOutlook(modelInput)
                .inheritMode(modelInput)
                .onSubmit(window)
                .setDefaultContent("10.0")
                .setId("$window");

        std.add(new HBox().add(new Label()
                .setContent("Min X")
                .inheritOutlook(windowModelLabel)
                .inheritDisplayProperties(windowModelLabel)).add(new TextInput()
                .inheritOutlook(modelInput)
                .inheritMode(modelInput)
                .onSubmit(window)
                .setDefaultContent("-10.0")
                .setId("$window")).setMargins(0, 0)
        );
        std.add(new HBox().add(windowModelLabel).add(windowModelTextInput).setMargins(0, 0));

        std.add(new HBox().add(new Label()
                .setContent("Min Y")
                .inheritOutlook(windowModelLabel)
                .inheritDisplayProperties(windowModelLabel)).add(new TextInput()
                .inheritOutlook(modelInput)
                .inheritMode(modelInput)
                .onSubmit(window)
                .setDefaultContent("-10.0")
                .setId("$window")).setMargins(0, 0)
        );
        std.add(new HBox().add(new Label()
                .setContent("Max Y")
                .inheritOutlook(windowModelLabel)
                .inheritDisplayProperties(windowModelLabel)).add(new TextInput()
                .inheritOutlook(modelInput)
                .inheritMode(modelInput)
                .onSubmit(window)
                .setDefaultContent("10.0")
                .setId("$window")).setMargins(0, 0)
        );


        std.add(new Button()
                .setContent("Equalize Axis")
                .onClick(graph::equalizeAxes)
                .inheritOutlook(modelButton)
                .inheritMode(modelButton));
        std.add(new Button()
                .setContent("Center Origin")
                .onClick(graph::centerOrigin)
                .inheritOutlook(modelButton)
                .inheritMode(modelButton));
        std.add(new SpaceHolder());
        std.add(new Label("Graph")
                .inheritOutlook(modelLabel));
        std.add(new Switch()
                .inheritOutlook(modelButton)
                .inheritMode(modelButton)
                .setContentOff("Trace Off")
                .setContentOn("Trace On")
                .setState(graph.tracingIsOn())
                .onClick(() -> graph.setTracingOn(!graph.tracingIsOn())));
        std.add(new Switch()
                .inheritOutlook(modelButton)
                .inheritMode(modelButton)
                .setContentOff("Axes Off")
                .setContentOn("Axes On")
                .setState(graph.isAxesVisible())
                .onClick(() -> graph.setAxesVisible(!graph.isAxesVisible())));
        std.add(new SpaceHolder());
        std.add(new Label("Evaluation")
                .inheritOutlook(modelLabel));
        std.add(new Switch()
                .inheritOutlook(modelButton)
                .inheritMode(modelButton)
                .setContentOff("Off")
                .setContentOn("On")
                .setState(graph.isEvaluationOn())
                .onClick(() -> graph.setEvaluationOn(!graph.isEvaluationOn())));
        std.add(new SpaceHolder());
        std.add(new Label("CAS")
                .inheritOutlook(modelLabel));
        std.add(new Switch()
                .inheritOutlook(modelButton)
                .inheritMode(modelButton)
                .setContentOff("Disabled")
                .setContentOn("Enabled")
                .setState(casEnabled)
                .onClick(() -> casEnabled = !casEnabled));
        std.add(new SpaceHolder());
        std.add(new Label("Control")
                .inheritOutlook(modelLabel));
        std.add(new Button()
                .setContent("Drag")
                .onClick(() -> graph.setMode(Graph.Mode.DRAG))
                .inheritOutlook(modelButton)
                .inheritMode(modelButton));
        std.add(new Button()
                .setContent("Zoom In")
                .onClick(() -> graph.setMode(Graph.Mode.ZOOM_IN))
                .inheritOutlook(modelButton)
                .inheritMode(modelButton));
        std.add(new Button()
                .setContent("Zoom Out")
                .onClick(() -> graph.setMode(Graph.Mode.ZOOM_OUT))
                .inheritOutlook(modelButton)
                .inheritMode(modelButton));
        std.add(new Button()
                .setContent("Zoom Rect")
                .onClick(() -> graph.setMode(Graph.Mode.ZOOM_RECT))
                .inheritOutlook(modelButton)
                .inheritMode(modelButton));

        std.add(new SpaceHolder());
        VBox funcsWrapper = new VBox(1.0f, 0.15f);
        funcsWrapper.setMargins(0, 0);
        for (int i = 0; i < 5; i++) {
            funcsWrapper.add(new Button()
                    .setVisible(false)
                    .inheritOutlook(modelButton)
                    .inheritMode(modelButton));
        }


        std.add(new Label()
                .setContent("Filter")
                .inheritOutlook(modelLabel));
        std.add(new TextInput()
                .inheritOutlook(modelInput)
                .inheritMode(modelInput)
                .setDefaultContent("Name")
                .onKeyTyped(() -> {
                    TextInput self = JNode.getTextInputById("filterer");
                    if (self == null) return;
                    ArrayList<Function> functions = graph.getFunctions();
                    ArrayList<Function> filtered = new ArrayList<>();
                    functions.forEach(function -> {
                        if (filtered.size() < 5 && function.getName().startsWith(self.getContent()))
                            if (!filtered.contains(function))
                                filtered.add(function);
                    });
                    functions.forEach(function -> {
                        if (filtered.size() < 5 && function.getName().contains(self.getContent()))
                            if (!filtered.contains(function))
                                filtered.add(function);
                    });
                    functions.forEach(function -> {
                        if (filtered.size() < 5 && containsAllChars(function.getName(), self.getContent()))
                            if (!filtered.contains(function))
                                filtered.add(function);
                    });
                    if (filtered.size() == 0) return;
                    for (int i = 0; i < 5; i++) {
                        if (i < filtered.size()) {
                            InterpretedFunction func = (InterpretedFunction) filtered.get(i);
                            final String funcName = func.getName();
                            final String operable = func.getOperable().toString();
                            Button tmp = (Button) funcsWrapper.getDisplayables().get(i);
                            tmp.setContent(funcName).setVisible(true);
                            tmp.onClick(() -> {
                                funcNameTextInput.setContent(funcName);
                                //noinspection ConstantConditions
                                JNode.getTextInputById("f(x)").setContent(operable);
                            });
                        } else {
                            funcsWrapper.getDisplayables().get(i).setVisible(false);
                        }
                    }
                }).setId("filterer"));
        std.add(funcsWrapper);


        std.add(new SpaceHolder());


        Button button = new Button("Show Advanced")
                .inheritOutlook(modelButton)
                .inheritMode(modelButton);
        button.attachMethod(() -> button.setVisible(graph.getFunctions().size() > 0));
        button.onClick(() -> {
            button.setContent(button.getContent().equals("Show Advanced") ? "Hide Advanced" : "Show Advanced");
            adv.setVisible(!adv.isVisible);
            parent.syncSize();
            parent.arrange();
        }).setAlign(CENTER).setVisible(false);
        std.add(button);
        std.add(new Button("Exit")
                .inheritOutlook(modelButton)
                .inheritMode(modelButton)
                .onClick(() -> Runtime.getRuntime().exit(0)));

        adv.add(new Label().setAlign(CENTER).setContent("Advanced").inheritOutlook(modelLabel));


        adv.add(new SpaceHolder());
        adv.add(new Label()
                .setContent("Attributes")
                .inheritOutlook(modelLabel));
        adv.add(new Switch()
                .inheritOutlook(modelButton)
                .inheritMode(modelButton)
                .setContentOn("Tracing Enabled")
                .setContentOff("Tracing Disabled")
                .onClick(() -> {
                    Switch self = (Switch) JNode.get("#8").get(0);
                    getCurrentFunction().setTracingEnabled(self.isOn());
                }).setId("#8"));

        Button showAsymptotes = new Button()
                .inheritOutlook(modelButton)
                .inheritMode(modelButton)
                .setContent("Show Asymptotes");
        showAsymptotes.onClick(() -> {
            Function function = getCurrentFunction();
            if (function != null)
                function.setAsymptoteVisible(!function.isAsymptoteVisible());
            showAsymptotes.setContent(showAsymptotes.getContent().equals("Show Asymptotes") ? "Hide Asymptotes" : "Show Asymptotes");
        }).setId("#1");
        adv.add(showAsymptotes);

        Button showTangentLine = new Button()
                .inheritOutlook(modelButton)
                .inheritMode(modelButton)
                .setContent("Show f'(x)");
        showTangentLine.onClick(() -> {
            Function function = getCurrentFunction();
            if (function != null)
                function.setTangentLineVisible(!function.isTangentLineVisible());
            showTangentLine.setContent(showTangentLine.getContent().equals("Show f'(x)") ? "Hide f'(x)" : "Show f'(x)");
        }).setId("#2");
        adv.add(showTangentLine);

        Button visibility = new Button()
                .inheritOutlook(modelButton)
                .inheritMode(modelButton)
                .setContent("Visible");
        visibility.onClick(() -> {
            Function function = getCurrentFunction();
            if (function != null)
                function.setVisible(!function.isVisible());
            visibility.setContent(visibility.getContent().equals("Visible") ? "Invisible" : "Visible");
        }).setId("#3");
        adv.add(visibility);

        Button continuous = new Button()
                .inheritOutlook(modelButton)
                .inheritMode(modelButton)
                .setContent("Continuous");
        continuous.onClick(() -> {
            Function function = getCurrentFunction();
            if (function != null)
                function.setGraphStyle(function.getStyle().equals(Function.Style.CONTINUOUS) ? Function.Style.DISCRETE : Function.Style.CONTINUOUS);
            continuous.setContent(continuous.getContent().equals("Continuous") ? "Discrete" : "Continuous");
        }).setId("#4");
        adv.add(continuous);

        Button dynamic = new Button()
                .inheritOutlook(modelButton)
                .inheritMode(modelButton)
                .setContent("Static");
        dynamic.onClick(() -> {
            Function function = getCurrentFunction();
            if (function != null)
                function.setDynamic(!function.isDynamic());
            dynamic.setContent(dynamic.getContent().equals("Static") ? "Dynamic" : "Static");
        }).setId("#5");
        adv.add(dynamic);

        adv.add(new SpaceHolder());
        adv.add(new Label().setContent("VA Extension")
                .inheritOutlook(modelLabel));
        Button extensionOn = new Button()
                .inheritOutlook(modelButton)
                .inheritMode(modelButton)
                .setContent("On");
        extensionOn.onClick(() -> {
            Function function = getCurrentFunction();
            if (function != null)
                function.setAutoAsymptoteExtension(!function.isAutoAsymptoteExtension());
            extensionOn.setContent(extensionOn.getContent().equals("On") ? "Off" : "On");
        }).setId("#7");
        adv.add(extensionOn);

        adv.add(new SpaceHolder());
        adv.add(new SpaceHolder());
        adv.add(new Label().setContent("Color")
                .inheritOutlook(modelLabel));

        ColorSelector functionColor = new ColorSelector(1.0f, 0.3f);
        functionColor.setId("#6");
        functionColor.getTitleWrapper().setRelativeH(0.13f);
        adv.add(functionColor);

        adv.add(new SpaceHolder());

        adv.add(new Label()
                .setContent("Additional")
                .inheritOutlook(modelLabel));
        adv.add(new Switch()
                .inheritOutlook(modelButton)
                .inheritMode(modelButton)
                .setContentOn("Differentiate")
                .setContentOff("Uniform").onClick(() -> {
                    Switch self = (Switch) JNode.get("#9").get(0);
                    getCurrentFunction().setMatchAuxiliaryLinesColor(self.isOn());
                }).setId("#9"));
        ValueSelector strokeWeight = new ValueSelector(1.0f, 0.057f);
        strokeWeight.setTitlePercentage(0.7f);
        strokeWeight.getTitleLabel()
                .setAlign(CENTER);
        strokeWeight.setRange(0.1f, 3)
                .setTitle("Stroke Weight")
                .setValue(1f)
                .link(() -> getCurrentFunction().setStrokeWeight(strokeWeight.getFloatValue())).setId("#0");
        adv.add(strokeWeight);
        adv.add(new SpaceHolder());
        adv.add(new Button()
                .inheritOutlook(modelButton)
                .inheritMode(modelButton)
                .setContent(JNode.ROUNDED ? "Rounded" : "Rectangular")
                .onClick(() -> {
                    JNode.getDisplayables().forEach(displayable -> displayable.setRounded(!displayable.isRounded));
                    Button self = (Button) JNode.get("UI-ROUNDING").get(0);
                    self.setContent(self.getContent().equals("Rounded") ? "Rectangular" : "Rounded");
                }).setId("UI-ROUNDING"));


        JNode.add(parent);

        UnaryOperation.define("~", Function.interpret("x*x"));
        BinaryOperation.define("%", 2, (a, b) -> a % b);
        Constants.define("$C", () -> 1);

        Element.getList().forEach(e -> Constants.define("$" + e, e::getAtomicMass));

        Constants.list();

        graph.equalizeAxes();
    }

    @SuppressWarnings("ConstantConditions")
    private Function getCurrentFunction() {
        return graph.getFunction(JNode.getTextInputById("funcNameTextInput").getContent());
    }

    private static Set<Character> stringToCharacterSet(String s) {
        Set<Character> set = new HashSet<>();
        for (char c : s.toCharArray()) {
            set.add(c);
        }
        return set;
    }

    private static boolean containsAllChars(String container, String contained) {
        return stringToCharacterSet(container).containsAll
                (stringToCharacterSet(contained));
    }

    public void draw() {
        background(52, 61, 70);
        JNode.run();
    }

    public void keyPressed() {
        switch (keyCode) {
            case 0x30:
                break;
            case ESC:
                key = (char) 0;
                break;
        }

        switch (key) {
            case TAB:
                functionInputWrapper.setVisible(!functionInputWrapper.isVisible());
                graphWrapper.syncSize();
                graphWrapper.arrange();
        }

        JNode.keyPressed();

    }

    public void keyReleased() {
        JNode.keyReleased();
    }

    public void mouseWheel() {
        //to be implemented. Jan 27th.
    }

}
