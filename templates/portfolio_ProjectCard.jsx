/**
 * ProjectCard.jsx — Reusable project card component for portfolio.
 *
 * Props:
 *   title       {string}   Project title
 *   description {string}   Short project description
 *   techStack   {string[]} List of technology names
 *   liveUrl     {string}   Live demo URL
 *   repoUrl     {string}   GitHub/source URL
 *   imageUrl    {string}   Project screenshot or thumbnail URL
 *   featured    {boolean}  Highlights card with accent border if true
 *
 * Add:    feat: add ProjectCard component with tech badges and hover effects
 * Delete: refactor: extract ProjectCard into ProjectGrid compound component
 */

import React, { useState } from 'react';
import PropTypes from 'prop-types';

// ─── Color palette for tech badges ──────────────────────────────────────────
const BADGE_COLORS = {
  React:        { bg: '#20232a', text: '#61dafb' },
  TypeScript:   { bg: '#007acc', text: '#ffffff' },
  JavaScript:   { bg: '#f7df1e', text: '#000000' },
  'Node.js':    { bg: '#3c873a', text: '#ffffff' },
  Python:       { bg: '#3776ab', text: '#ffd343' },
  Java:         { bg: '#f89820', text: '#ffffff' },
  'Spring Boot':{ bg: '#6db33f', text: '#ffffff' },
  Docker:       { bg: '#0db7ed', text: '#ffffff' },
  AWS:          { bg: '#ff9900', text: '#232f3e' },
  MongoDB:      { bg: '#4db33d', text: '#ffffff' },
  PostgreSQL:   { bg: '#336791', text: '#ffffff' },
  Redis:        { bg: '#d82c20', text: '#ffffff' },
  GraphQL:      { bg: '#e10098', text: '#ffffff' },
  Kubernetes:   { bg: '#326ce5', text: '#ffffff' },
  Tailwind:     { bg: '#06b6d4', text: '#ffffff' },
  default:      { bg: '#334155', text: '#94a3b8' },
};

function TechBadge({ name }) {
  const colors = BADGE_COLORS[name] || BADGE_COLORS.default;
  return (
    <span
      style={{
        backgroundColor: colors.bg,
        color: colors.text,
        padding: '3px 10px',
        borderRadius: '9999px',
        fontSize: '0.72rem',
        fontWeight: 600,
        letterSpacing: '0.02em',
        display: 'inline-block',
        margin: '3px 3px 0 0',
        fontFamily: 'inherit',
      }}
    >
      {name}
    </span>
  );
}

TechBadge.propTypes = {
  name: PropTypes.string.isRequired,
};

function ProjectCard({
  title,
  description,
  techStack,
  liveUrl,
  repoUrl,
  imageUrl,
  featured,
}) {
  const [hovered, setHovered] = useState(false);

  const cardStyle = {
    borderRadius: '16px',
    overflow: 'hidden',
    background: hovered
      ? 'linear-gradient(145deg, #1e293b, #0f172a)'
      : 'linear-gradient(145deg, #1a2437, #0d1520)',
    border: featured
      ? `1.5px solid ${hovered ? '#818cf8' : '#6366f1'}`
      : `1px solid ${hovered ? '#334155' : '#1e293b'}`,
    boxShadow: hovered
      ? '0 20px 60px rgba(99,102,241,0.25), 0 8px 20px rgba(0,0,0,0.4)'
      : '0 4px 20px rgba(0,0,0,0.3)',
    transition: 'all 0.35s cubic-bezier(0.4, 0, 0.2, 1)',
    transform: hovered ? 'translateY(-6px) scale(1.01)' : 'translateY(0) scale(1)',
    cursor: 'default',
    display: 'flex',
    flexDirection: 'column',
    maxWidth: '420px',
    width: '100%',
    position: 'relative',
  };

  const imageStyle = {
    width: '100%',
    height: '200px',
    objectFit: 'cover',
    display: 'block',
    filter: hovered ? 'brightness(1.05)' : 'brightness(0.9)',
    transition: 'filter 0.35s ease',
  };

  const contentStyle = {
    padding: '20px 22px 22px',
    display: 'flex',
    flexDirection: 'column',
    gap: '10px',
    flexGrow: 1,
  };

  return (
    <div
      style={cardStyle}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      role="article"
      aria-label={`Project: ${title}`}
    >
      {/* Featured badge */}
      {featured && (
        <div
          style={{
            position: 'absolute',
            top: '12px',
            right: '12px',
            background: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
            color: '#fff',
            fontSize: '0.65rem',
            fontWeight: 700,
            padding: '3px 10px',
            borderRadius: '9999px',
            letterSpacing: '0.08em',
            textTransform: 'uppercase',
            zIndex: 2,
          }}
        >
          ★ Featured
        </div>
      )}

      {/* Project image */}
      {imageUrl && (
        <img src={imageUrl} alt={`${title} screenshot`} style={imageStyle} />
      )}

      {/* Card content */}
      <div style={contentStyle}>
        <h3
          style={{
            margin: 0,
            fontSize: '1.15rem',
            fontWeight: 700,
            color: hovered ? '#e2e8f0' : '#cbd5e1',
            transition: 'color 0.25s',
            fontFamily: "'Inter', 'Outfit', sans-serif",
          }}
        >
          {title}
        </h3>

        <p
          style={{
            margin: 0,
            fontSize: '0.875rem',
            color: '#94a3b8',
            lineHeight: 1.65,
            flexGrow: 1,
          }}
        >
          {description}
        </p>

        {/* Tech stack badges */}
        {techStack && techStack.length > 0 && (
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0' }}>
            {techStack.map((tech) => (
              <TechBadge key={tech} name={tech} />
            ))}
          </div>
        )}

        {/* Action buttons */}
        <div style={{ display: 'flex', gap: '10px', marginTop: '8px' }}>
          {liveUrl && (
            <a
              href={liveUrl}
              target="_blank"
              rel="noopener noreferrer"
              id={`live-btn-${title.replace(/\s+/g, '-').toLowerCase()}`}
              style={{
                padding: '8px 18px',
                borderRadius: '8px',
                background: hovered
                  ? 'linear-gradient(135deg, #6366f1, #8b5cf6)'
                  : 'linear-gradient(135deg, #4f46e5, #7c3aed)',
                color: '#fff',
                fontSize: '0.8rem',
                fontWeight: 600,
                textDecoration: 'none',
                transition: 'background 0.25s, transform 0.2s',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '5px',
              }}
            >
              🚀 Live Demo
            </a>
          )}
          {repoUrl && (
            <a
              href={repoUrl}
              target="_blank"
              rel="noopener noreferrer"
              id={`repo-btn-${title.replace(/\s+/g, '-').toLowerCase()}`}
              style={{
                padding: '8px 18px',
                borderRadius: '8px',
                background: 'transparent',
                color: '#94a3b8',
                fontSize: '0.8rem',
                fontWeight: 600,
                textDecoration: 'none',
                border: '1px solid #334155',
                transition: 'border-color 0.25s, color 0.25s',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '5px',
              }}
            >
              ⚡ Source
            </a>
          )}
        </div>
      </div>
    </div>
  );
}

ProjectCard.propTypes = {
  title:       PropTypes.string.isRequired,
  description: PropTypes.string.isRequired,
  techStack:   PropTypes.arrayOf(PropTypes.string),
  liveUrl:     PropTypes.string,
  repoUrl:     PropTypes.string,
  imageUrl:    PropTypes.string,
  featured:    PropTypes.bool,
};

ProjectCard.defaultProps = {
  techStack: [],
  liveUrl:   null,
  repoUrl:   null,
  imageUrl:  null,
  featured:  false,
};

export default ProjectCard;
