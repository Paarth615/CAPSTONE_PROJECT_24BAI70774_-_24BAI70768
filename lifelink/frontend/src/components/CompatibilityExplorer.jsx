import { useState } from 'react';
import { BLOOD_GROUPS, canDonate } from '../utils/constants';
import { BloodTag } from './ui';

/**
 * Landing-page centrepiece: pick a blood group and see exactly who it can give to,
 * or who it can receive from. The same ABO/Rh rule the server uses for matching.
 */
export default function CompatibilityExplorer() {
  const [group, setGroup] = useState('B+');
  const [mode, setMode] = useState('give'); // give | receive

  const matches = BLOOD_GROUPS.filter((g) => (mode === 'give' ? canDonate(group, g) : canDonate(g, group)));

  let summary;
  if (mode === 'give') {
    summary =
      matches.length === 8
        ? `${group} can give to every blood group.`
        : matches.length === 1
        ? `${group} can only give to ${group}.`
        : `${group} can give to ${matches.join(', ')}.`;
  } else {
    summary =
      matches.length === 8
        ? `${group} can receive from every blood group.`
        : matches.length === 1
        ? `${group} can only receive from ${group}.`
        : `${group} can receive from ${matches.join(', ')}.`;
  }

  return (
    <section className="explorer" aria-labelledby="explorer-title">
      <h2 id="explorer-title">Who can give to whom?</h2>
      <p className="explorer-sub">Choose a blood group.</p>

      <div className="explorer-groups" role="radiogroup" aria-label="Blood group">
        {BLOOD_GROUPS.map((g) => (
          <button
            key={g}
            type="button"
            role="radio"
            aria-checked={group === g}
            className={`explorer-pick${group === g ? ' is-picked' : ''}`}
            onClick={() => setGroup(g)}
          >
            <BloodTag group={g} size="md" state={group === g ? 'solid' : undefined} />
          </button>
        ))}
      </div>

      <div className="segmented" role="tablist" aria-label="Direction">
        <button type="button" role="tab" aria-selected={mode === 'give'} className={mode === 'give' ? 'on' : ''} onClick={() => setMode('give')}>
          Can give to
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={mode === 'receive'}
          className={mode === 'receive' ? 'on' : ''}
          onClick={() => setMode('receive')}
        >
          Can receive from
        </button>
      </div>

      <div className="explorer-result" aria-live="polite">
        <div className="explorer-row">
          {BLOOD_GROUPS.map((g) => (
            <BloodTag key={g} group={g} size="sm" state={matches.includes(g) ? 'solid' : 'faded'} />
          ))}
        </div>
        <p className="explorer-summary">{summary}</p>
      </div>
    </section>
  );
}
