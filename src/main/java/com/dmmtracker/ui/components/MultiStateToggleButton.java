/*
 * Copyright (c) 2026, DMMScape
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.dmmtracker.ui.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Icon;
import javax.swing.JButton;

public class MultiStateToggleButton extends JButton
{
	private final Icon[] icons;
	private final String[] tooltips;
	private final int stateCount;
	private int state = 0;
	private ActionListener stateChangedAction;

	public MultiStateToggleButton(int stateCount)
	{
		super();
		this.stateCount = stateCount;
		this.icons = new Icon[stateCount];
		this.tooltips = new String[stateCount];
		addActionListener(e -> setStateThenAction((state + 1) % stateCount));
	}

	public int getState()
	{
		return state;
	}

	public void setStateChangedAction(ActionListener listener)
	{
		this.stateChangedAction = listener;
	}

	public void setIcon(Icon icon, int state)
	{
		if (state < 0 || state >= stateCount || icon == null)
		{
			return;
		}
		icons[state] = icon;
		if (state == this.state)
		{
			updateIcon();
		}
	}

	public void setToolTip(String tooltip, int state)
	{
		if (state < 0 || state >= stateCount || tooltip == null)
		{
			return;
		}
		tooltips[state] = tooltip;
		if (state == this.state)
		{
			updateTooltip();
		}
	}

	public void setState(int state)
	{
		if (state < 0 || state >= stateCount)
		{
			return;
		}
		this.state = state;
		updateIcon();
		updateTooltip();
	}

	public void setStateThenAction(int state)
	{
		setState(state);
		if (stateChangedAction != null)
		{
			stateChangedAction.actionPerformed(new ActionEvent(this, 0, ""));
		}
	}

	private void updateIcon()
	{
		super.setIcon(icons[state]);
	}

	private void updateTooltip()
	{
		super.setToolTipText(tooltips[state]);
	}
}
