DifInstrument {                                                            //create an instrument an its variables
	var <>name, <>point, <>fileBufnum, <>synthNode, <>listenerPoint, <>distance, <>azimuth, <>angle, <>gain;

	*new { arg nameIn, pointIn, fileBufnumIn, listenerPointIn;             //Arguments ("string" , Point(), file.bufnum, Point())
			^super.new.init(nameIn, pointIn, fileBufnumIn, listenerPointIn);
	}

	//initiates the instrument and calculates the Synth parameters for this instrument's position in relation with listener
	init { arg nameIn, pointIn, fileBufnumIn, listenerPointIn;
		name = nameIn;
		point = pointIn;
		fileBufnum = fileBufnumIn;
		listenerPoint = listenerPointIn;
		this.calculate;
	}

	//calculates parameters to be used in synth
	calculate {
		distance = (point - listenerPoint).rho;           // distance between instrument and listener
		azimuth = ((point - listenerPoint).theta)-(pi/2); // azimuth - angle of instrument in relation listener (location) facing the stage
		//azimuth = (((point - listenerPoin).theta)-pi).wrap(-pi, pi)// facing left
		angle = distance.linlin(200, 3740, pi/2, pi/20);  // dispersion is mapped to the distance. min-Max depend of hall
		gain = distance.linlin(200, 3740, 1, 0.1);        // volumen is mapped to the distance. min-Max depend of hall
	}                                                     // min-Max = minimum and maximum distance between instr-listener

	//Updates the synth arguments values depending on new position when the synth is declared
	updateNode {
		if (synthNode.isNil.not) {
		synthNode.set(\azim, azimuth, \angle, angle, \distan, distance, \gain, gain);
		}
	}
	//Calculates and update the synth arguments' values when the listener's position is changed
	updateListener {arg listenerPointIn;
		listenerPoint = listenerPointIn;
		this.calculate;
		this.updateNode;

	}
	//Calculates and update the synth arguments' values when the instrument's position is changed
	move { arg pointIn;
		point = pointIn;
		this.calculate;
		this.updateNode;
	}

	//start the synth with the given values
	start {arg out, ampBus;
		synthNode = Synth.new("model", [\out, out, \buffer, fileBufnum, \azim, azimuth, \angle, angle, \distan, distance, \gain, gain, \ampBus, ampBus]);
	}
	//stop the synth and erase the node
	stop {
		synthNode.set(\end,1);
		synthNode = nil;
	}
}

DifOrchestra {
	var <>instrumentList, <>listener, <>playing, <>decoderNode, <>ambisonicsBus, <>ampBus; // creates variables for Orchestra

	*new { arg listenerPoint;
		^super.new.init(listenerPoint);
	}

	init { arg listenerPoint;                   //argument the listener position (Point())
		listener = listenerPoint;
		instrumentList = [];
		playing = false;
		ambisonicsBus = Bus.audio(Server.local, 4);
		ampBus = Bus.control(Server.local, 1);
	}

	setAmp {arg amp;
		ampBus.set(amp);
	}

	//add an instrument to the instrumets' list of the orchestra. Arguments ("name", Point(x, y), ~audio.bufnum)
	addInstr {arg nameIn, pointIn, fileBufnumIn;
		instrumentList = instrumentList.add(DifInstrument.new(nameIn, pointIn, fileBufnumIn, listener));
	}

	//add an several instruments (array) at the same time. Arguments list of list ([["name", Point(), ~audio.bufnum], [], []])
	addInstrList {arg list;
		list.do({arg i; this.addInstr(i[0], i[1], i[2])});
	}

	//delete an instrument from the list by name. Argument ("name")
	deleteInstr {arg nameIn;
		instrumentList = instrumentList.reject({arg i; if (nameIn == i.name) {
			i.stop;
			true;
		} {
			false;
		}
		});
	}

	//move (calculates and update position) of an instrument from the list by name. Argument("name", Point(x,y))
	moveInstr { arg nameIn, pointIn;
		instrumentList.detect({arg i; nameIn == i.name}).move(pointIn);
	}

	//move (calculates and update position) of listener. Argument (Point(x,y))
	moveListener { arg listenerNew;
		listener = listenerNew;
		instrumentList.do({arg i; i.updateListener(listenerNew)});
	}

	//start the synth with the given values for all the instruments
	start {
		if (playing.not) {
			decoderNode = Synth("renderDecoder", [\in, ambisonicsBus.index]);
			instrumentList.do{arg i; i.start(ambisonicsBus.index, ampBus.index)};
			playing = true;
		}
	}

	//stop all the instruments
	stop {
		if (playing) {
			instrumentList.do{arg i; i.stop};
			playing = false;
			decoderNode.free;
		}
	}

	displayGUI {
		var pro, marginX, marginY, galeriaX, hallX, hallY, win, menu, boxX, boxY, boxAction, hallFunc, orchestra, boxLisX, boxLisY, listenerArea, mapCoordinates, instrumentFunc, buttons, start, ampSlider;
		pro = 0.2; //proportion 100 cm = 20 pixels
		marginX = 60;
		marginY = 60;
		galeriaX = 800*pro; //widht of the gallerie
		hallX = 3500*pro; // width of the hall + gallerie
		hallY = 4400*pro; // length of the hall
		orchestra = this;

		Window.closeAll;
		win = Window("Model_Spatialization",Rect(1064,164,1100,1000), scroll:true);
		StaticText(win,Rect(20,5,300,40)).string_("Sound Spatialization Model").font_(Font(Font.defaultSansFace,16, true, true));
		StaticText(win,Rect(930,5,200,40)).string_("© 2018 Lula Romero").font_(Font(Font.defaultSansFace,14));
		win.view.background_(Color.white);

		hallFunc = {
			Pen.color_(Color.white);
			Pen.strokeColor_(Color.black);
			Pen.addRect(Rect(marginX, marginY, hallX, hallY)); // hall area + galery
			Pen.addRect(Rect(marginX+galeriaX+(350*pro), marginY+(200*pro), 2000*pro, 1300*pro)); // stage
			Pen.line((marginX+galeriaX)@marginY, (marginX+galeriaX)@(marginY+(hallY)));  //galery
			Pen.line(marginX@(marginY+(1200*pro)), (marginX+galeriaX)@(marginY+(1200*pro)));
			Pen.line(marginX@(marginY+(1450*pro)), (marginX+galeriaX)@(marginY+(1450*pro)));
			Pen.line(marginX@(marginY+(2950*pro)), (marginX+galeriaX)@(marginY+(2950*pro)));
			Pen.line(marginX@(marginY+(3200*pro)), (marginX+galeriaX)@(marginY+(3200*pro)));
			Pen.fillStroke;
		};

		boxLisX = NumberBox(win, Rect((marginX+hallX + 30), marginY+340, 80, 30));
		boxLisY = NumberBox(win, Rect((marginX+hallX + 30), marginY+380, 80, 30));
		StaticText(win,Rect((marginX+hallX + 30),marginY+290,100,50)).string_("Listener's position").font_(Font(Font.defaultSansFace,16));
		StaticText(win,Rect((marginX+hallX + 130),marginY+340,40,30)).string_("X").font_(Font(Font.defaultSansFace,16));
		StaticText(win,Rect((marginX+hallX + 130),marginY+380,40,30)).string_("Y").font_(Font(Font.defaultSansFace,16));

		listenerArea = Slider2D(win, Rect(marginX+galeriaX+(170*pro), marginY+(1700*pro),2360*pro,2300*pro)).background_(Color.new255(220,252, 255)).knobColor_(Color.new255(86,170, 17)).action_({arg coord;
			var newX, newY;
			newX = coord.x.linlin(0, 1, 970, 3330);
			newY = coord.y.linlin(0,1, 400, 2700);
			orchestra.moveListener(Point(newX, newY));
			boxLisX.value_(newX);
			boxLisY.value_(newY);
		});
		listenerArea.x = orchestra.listener.x.linlin(970, 3330, 0, 1);
		listenerArea.y = orchestra.listener.y.linlin(400, 2700, 0, 1);

		boxLisX.decimals_(0).value = listenerArea.x.linlin(0,1 , 970, 3330);
		boxLisY.decimals_(0).value = listenerArea.y.linlin(0, 1, 400, 2700);

		mapCoordinates = {arg point; Point((point.x*pro)+marginX, ((point.y*pro)-hallY).abs+marginY)};

		instrumentFunc = {arg instrument;
			var name, mappedPoint;
			name = instrument.name;
			mappedPoint = mapCoordinates.value(instrument.point);
			Button(win, Rect(mappedPoint.x-10, mappedPoint.y-10, 20,20)).states_([[name, Color.black, Color.new255(246,126, 237)], [name, Color.black, Color.grey]]).font_(Font(Font.defaultSansFace,8));
		};

		start = Button(win, Rect((marginX+hallX + 250), marginY+100, 60,60)).states_([["START", Color.black, Color.new255(100,200, 17)], ["STOP", Color.black, Color.grey]]).font_(Font(Font.defaultSansFace,8)).action_({ arg butt;
			var butVal = butt.value;
			if (butVal == 1)
			{orchestra.start}
			{orchestra.stop}
		});

		ampSlider = Slider(win, Rect((marginX+hallX+100), marginY+140, 50,100)).background_(Color.new255(253,255, 153)).knobColor_(Color.new255(86,170, 17));
		StaticText(win,Rect((marginX+hallX + 85),marginY+110,100,30)).string_("AMPLITUDE").font_(Font(Font.defaultSansFace,16));
		ampSlider.action = {
			arg slider;
			var num = slider.value.lincurve(0,1,0,1,2);
			orchestra.setAmp(num);
		};
		boxX = NumberBox(win, Rect((marginX+hallX + 240), marginY, 80, 30));
		StaticText(win,Rect((marginX+hallX + 220),marginY,40,30)).string_("X").font_(Font(Font.defaultSansFace,16));
		boxY = NumberBox(win, Rect((marginX+hallX + 240), marginY+50, 80, 30));
		StaticText(win,Rect((marginX+hallX + 220),marginY+50,40,30)).string_("Y").font_(Font(Font.defaultSansFace,16));
		boxAction =  {
			var newLocation, newLocationMapped;
			newLocation = Point(boxX.value, boxY.value);
			newLocationMapped = mapCoordinates.value(newLocation);
			orchestra.instrumentList[menu.value].move(newLocation);
			buttons[menu.value].moveTo(newLocationMapped.x-10, newLocationMapped.y-10);
		};
		boxX.action = boxAction;
		boxY.action = boxAction;

		win.drawFunc = {hallFunc.value();};
		buttons = orchestra.instrumentList.collect(instrumentFunc);
		buttons.do({arg but,i; but.action_({ arg butt;
			var butVal, point, index;
			butVal = but.value;
			if ((butVal == 1), {
				buttons.do({arg b; b.value_(0)});
				but.value_(1);
				index = orchestra.instrumentList.detectIndex({arg instr; instr.name == but.states[0][0]});
				menu.valueAction = index;
			});
		})});

		menu = PopUpMenu(win, Rect (marginX+hallX + 30, marginY, 150, 40));
		menu.items = orchestra.instrumentList.collect{arg i; i.name};
		menu.action = {arg item;
			var point = orchestra.instrumentList[item.value].point;
			boxX.value = point.x;
			boxY.value = point.y;
		};


		win.alwaysOnTop_(false);
		win.front;
	}
}
