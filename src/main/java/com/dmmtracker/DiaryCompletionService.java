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

package com.dmmtracker;

import com.dmmtracker.data.DataLoader;
import com.dmmtracker.data.DiaryData;
import com.dmmtracker.data.DiaryTaskCompletionRule;
import com.dmmtracker.data.DiaryTaskData;
import com.dmmtracker.data.DiaryTierData;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public class DiaryCompletionService
{
	private static final Logger log = LoggerFactory.getLogger(DiaryCompletionService.class);

	private final Client client;
	private final DataLoader dataLoader;
	private final Map<String, Integer> varpCache = new HashMap<>();
	private final Map<String, Integer> varbitCache = new HashMap<>();
	private List<CompletionCheck> checks = Collections.emptyList();

	@Inject
	public DiaryCompletionService(Client client, DataLoader dataLoader)
	{
		this.client = client;
		this.dataLoader = dataLoader;
		rebuild();
		dataLoader.addUpdateListener(this::rebuild);
	}

	public synchronized void rebuild()
	{
		List<CompletionCheck> next = new ArrayList<>();
		for (DiaryData diary : dataLoader.getDiaries())
		{
			addTierChecks(diary.getTiers().getEasy(), next);
			addTierChecks(diary.getTiers().getMedium(), next);
			addTierChecks(diary.getTiers().getHard(), next);
			addTierChecks(diary.getTiers().getElite(), next);
		}
		checks = next;
	}

	public Set<String> getCompletedTasks()
	{
		List<CompletionCheck> snapshot;
		synchronized (this)
		{
			snapshot = new ArrayList<>(checks);
		}

		Set<String> completed = new HashSet<>();
		for (CompletionCheck check : snapshot)
		{
			int value = check.isVarp
				? client.getVarpValue(check.varId)
				: client.getVarbitValue(check.varId);
			if (check.isComplete(value))
			{
				completed.add(check.taskId);
			}
		}
		return completed;
	}

	private void addTierChecks(DiaryTierData tier, List<CompletionCheck> out)
	{
		if (tier == null)
		{
			return;
		}

		for (DiaryTaskData task : tier.getTasks())
		{
			if (task == null || task.getId() == null)
			{
				continue;
			}
			DiaryTaskCompletionRule rule = task.getCompletion();
			if (rule == null || rule.getType() == null)
			{
				continue;
			}

			CompletionCheck check = buildCheck(task.getId(), rule);
			if (check != null)
			{
				out.add(check);
			}
		}
	}

	private CompletionCheck buildCheck(String taskId, DiaryTaskCompletionRule rule)
	{
		switch (rule.getType())
		{
			case "varp_bit":
				return buildBitCheck(taskId, true, rule.getVarp(), rule.getBit(), rule.getIsSet());
			case "varbit_bit":
				return buildBitCheck(taskId, false, rule.getVarbit(), rule.getBit(), rule.getIsSet());
			case "varbit":
				return buildVarbitCheck(taskId, rule.getVarbit(), rule.getOp(), rule.getValue());
			default:
				return null;
		}
	}

	private CompletionCheck buildBitCheck(String taskId, boolean isVarp, String name, Integer bit, Boolean isSet)
	{
		if (name == null || bit == null || isSet == null)
		{
			return null;
		}

		int varId = isVarp ? resolveVarpId(name) : resolveVarbitId(name);
		if (varId < 0)
		{
			return null;
		}

		return new CompletionCheck(taskId, isVarp, varId, RuleType.BIT, bit, isSet, null, 0);
	}

	private CompletionCheck buildVarbitCheck(String taskId, String name, String op, Integer value)
	{
		if (name == null || op == null || value == null)
		{
			return null;
		}

		int varId = resolveVarbitId(name);
		if (varId < 0)
		{
			return null;
		}

		return new CompletionCheck(taskId, false, varId, RuleType.OP, 0, false, op, value);
	}

	private int resolveVarpId(String name)
	{
		if (name == null)
		{
			return -1;
		}

		Integer cached = varpCache.get(name);
		if (cached != null)
		{
			return cached;
		}

		int id = resolveStaticInt(VarPlayerID.class, name);
		varpCache.put(name, id);
		return id;
	}

	private int resolveVarbitId(String name)
	{
		if (name == null)
		{
			return -1;
		}

		Integer cached = varbitCache.get(name);
		if (cached != null)
		{
			return cached;
		}

		int id = resolveStaticInt(VarbitID.class, name);
		varbitCache.put(name, id);
		return id;
	}

	private int resolveStaticInt(Class<?> source, String name)
	{
		try
		{
			Field field = source.getField(name);
			return field.getInt(null);
		}
		catch (Exception e)
		{
			log.debug("Missing {} id for {}", source.getSimpleName(), name);
			return -1;
		}
	}

	private enum RuleType
	{
		BIT,
		OP
	}

	private static class CompletionCheck
	{
		private final String taskId;
		private final boolean isVarp;
		private final int varId;
		private final RuleType ruleType;
		private final int bit;
		private final boolean isSet;
		private final String op;
		private final int value;

		private CompletionCheck(String taskId, boolean isVarp, int varId, RuleType ruleType, int bit, boolean isSet, String op, int value)
		{
			this.taskId = taskId;
			this.isVarp = isVarp;
			this.varId = varId;
			this.ruleType = ruleType;
			this.bit = bit;
			this.isSet = isSet;
			this.op = op;
			this.value = value;
		}

		private boolean isComplete(int current)
		{
			switch (ruleType)
			{
				case BIT:
					if (bit < 0 || bit > 31)
					{
						return false;
					}
					boolean set = ((current >> bit) & 1) == 1;
					return set == isSet;
				case OP:
					return compare(current);
				default:
					return false;
			}
		}

		private boolean compare(int current)
		{
			if (op == null)
			{
				return false;
			}

			switch (op)
			{
				case "==":
					return current == value;
				case "!=":
					return current != value;
				case "<":
					return current < value;
				case "<=":
					return current <= value;
				case ">":
					return current > value;
				case ">=":
					return current >= value;
				default:
					return false;
			}
		}
	}
}
