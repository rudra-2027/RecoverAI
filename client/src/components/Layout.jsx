import React from 'react';
import { NavLink, Outlet } from 'react-router-dom';

export default function Layout() {
  const navItems = [
    { name: 'Dashboard', path: '/', icon: 'dashboard' },
    { name: 'Failed Mandates', path: '/failed-mandates', icon: 'assignment_late' },
    { name: 'Recovery Decisions', path: '/recovery-decisions', icon: 'psychology' },
    { name: 'Recovery Outcomes', path: '/recovery-outcomes', icon: 'check_circle' },
    { name: 'Audit Trail', path: '/audit-trail', icon: 'history' },
    { name: 'Batch Processing', path: '/batch-processing', icon: 'inventory_2' },
    { name: 'AI Insights', path: '/ai-insights', icon: 'auto_awesome' },
    { name: 'Demo Simulator', path: '/demo-simulator', icon: 'precision_manufacturing' },
    //{ name: 'Settings', path: '/settings', icon: 'settings' }
  ];

  return (
    <div className="h-screen overflow-hidden flex w-full bg-background text-on-surface font-body-md">
      {/* SideNavBar */}
      <nav className="w-64 h-screen fixed left-0 top-0 bg-inverse-surface shadow-md flex flex-col py-6 px-4 z-20">
        <div className="mb-8 px-4 flex items-center gap-3">
          <div className="w-10 h-10 rounded-lg bg-primary-fixed flex items-center justify-center">
            <img 
              className="w-8 h-8 object-contain rounded" 
              alt="RecoverAI Logo"
              src="https://lh3.googleusercontent.com/aida-public/AB6AXuBbQqyFeoEqqwDySjQVX19P-Z-nbJPGOeVslI3x7M-etuLOlwT9q2sHV1aP0Ndv56vKmtj4bXXC-SNfuKWGIE7Fcxl-m-GO4VSN-EuX2FxkNDt7dpPW_GcDz9Ok7LuVjzDjKj3llB3JDBZsltlv-HzNjqoInitX78ZRKNkDELrQo2Owqv2rIMeQ067DjSeMYVPHeLI9XW7W8phN93g6mVGgpxARudtG50VxWUDZivsv8V5QJesrT_KE"
            />
          </div>
          <div>
            <h1 className="font-display text-[24px] leading-tight font-bold text-on-primary-fixed-variant tracking-tight">RecoverAI</h1>
            <p className="font-label-md text-label-md text-outline-variant">Enterprise Recovery</p>
          </div>
        </div>

        <ul className="flex-1 space-y-2">
          {navItems.map((item) => (
            <li key={item.path}>
              <NavLink
                to={item.path}
                className={({ isActive }) =>
                  `flex items-center gap-3 px-4 py-3 rounded-lg font-title-md text-title-md transition-colors ${
                    isActive
                      ? 'text-primary-fixed bg-on-secondary-fixed-variant font-bold opacity-90'
                      : 'text-outline-variant hover:text-white hover:bg-on-secondary-fixed-variant'
                  }`
                }
              >
                {({ isActive }) => (
                  <>
                    <span className={`material-symbols-outlined ${isActive ? 'fill' : ''}`}>
                      {item.icon}
                    </span>
                    <span>{item.name}</span>
                  </>
                )}
              </NavLink>
            </li>
          ))}
        </ul>

        <div className="mt-auto pt-4 border-t border-outline/20">
          <NavLink
            to="/settings"
            className={({ isActive }) =>
              `flex items-center gap-3 px-4 py-3 rounded-lg font-title-md text-title-md transition-colors ${
                isActive
                  ? 'text-primary-fixed bg-on-secondary-fixed-variant font-bold opacity-90'
                  : 'text-outline-variant hover:text-white hover:bg-on-secondary-fixed-variant'
              }`
            }
          >
            {({ isActive }) => (
              <>
                <span className={`material-symbols-outlined ${isActive ? 'fill' : ''}`}>
                  settings
                </span>
                <span>Settings</span>
              </>
            )}
          </NavLink>
        </div>
      </nav>

      {/* Main Content Area */}
      <div className="flex-1 ml-64 flex flex-col h-screen overflow-hidden">
        {/* TopNavBar */}
        <header className="bg-surface flex justify-between items-center h-16 px-container-padding z-10 sticky top-0 border-b border-outline-variant/30 flex-shrink-0">
          <div className="flex items-center">
            {/* Search placeholder or breadcrumb */}
          </div>
          <div className="flex items-center gap-6">
            <div className="text-on-surface-variant font-label-md text-label-md bg-surface-container py-1 px-3 rounded-full border border-outline-variant/30">
              Environment: Production
            </div>
            {/* <div className="flex items-center gap-4 text-on-surface-variant">
              <button className="hover:text-primary transition-all p-1 rounded-full hover:bg-surface-container">
                <span className="material-symbols-outlined">notifications</span>
              </button>
              <button className="hover:text-primary transition-all p-1 rounded-full hover:bg-surface-container">
                <span className="material-symbols-outlined">tune</span>
              </button>
            </div> */}
            {/* <div className="w-8 h-8 rounded-full overflow-hidden border border-outline-variant/50">
              <img 
                alt="User Profile" 
                className="w-full h-full object-cover" 
                src="https://lh3.googleusercontent.com/aida-public/AB6AXuCP8XWHG29iV4UsFCsyznYiiormOuag07xAxQtmBAtWky_Yf6y9WQnIuLi5gEa64hanFjJrYaA64hLaxAAs6SleyycZgyfmfNc0wf6a45iWVINAt9N3o-FmtESK6VrimPttNzD9HBQ2qk42QOZRZlLxAR4oMKRooZa5L_RnBVvtjZ7t4qgy0w7RfgfgymQPmW2W3vSqga9f3QeaIkBEtSj9XOdBjGlMXAsXl0cDNXCKXzx9CuQ_EhhW"
              />
            </div> */}
          </div>
        </header>

        {/* Content canvas */}
        <main className="flex-1 overflow-y-auto bg-background flex flex-col min-h-0">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
