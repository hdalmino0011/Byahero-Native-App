import React, {useMemo, useState} from 'react';
import {CheckCircle2, Clock, MapPin, Navigation, RotateCcw, XCircle} from 'lucide-react';
import {RideBooking} from '../types/byahero';
import {formatPhp} from '../utils/pricing';
import {ByaheroMotorcycleVector, ByaheroTaxiVector} from './VectorGraphics';

interface OrderHistoryPanelProps {
  orders: RideBooking[];
  onRebookOrder: (order: RideBooking) => void;
}

type HistoryFilter = 'all' | 'completed' | 'cancelled';

export const OrderHistoryPanel: React.FC<OrderHistoryPanelProps> = ({
  orders,
  onRebookOrder,
}) => {
  const [filter, setFilter] = useState<HistoryFilter>('all');

  const completedCount = useMemo(
    () => orders.filter((o) => o.status === 'completed').length,
    [orders]
  );

  const totalSpent = useMemo(
    () =>
      orders
        .filter((o) => o.status === 'completed' && o.roleContext !== 'rider')
        .reduce((sum, o) => sum + o.fare.finalFare, 0),
    [orders]
  );

  const filteredOrders = useMemo(() => {
    if (filter === 'all') return orders;
    return orders.filter((o) => o.status === filter);
  }, [orders, filter]);

  const formatTimestamp = (iso: string) => {
    const dt = new Date(iso);
    if (Number.isNaN(dt.getTime())) return 'Recent trip';
    return dt.toLocaleString(undefined, {
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    });
  };

  return (
    <div className="space-y-4">
      {/* Summary Header Cards */}
      <div className="grid grid-cols-2 gap-2.5">
        <div className="p-3.5 rounded-2xl glass-green text-white">
          <div className="text-[11px] font-medium text-emerald-100">Completed Orders</div>
          <div className="text-xl font-extrabold text-white mt-0.5 tabular-nums">
            {completedCount}
          </div>
          <div className="text-[10px] text-emerald-200 mt-0.5">
            {orders.length} total logged
          </div>
        </div>
        <div className="p-3.5 rounded-2xl glass-surface">
          <div className="text-[11px] font-medium text-[#55625A]">Ride Spend</div>
          <div className="text-xl font-extrabold text-[#101713] mt-0.5 tabular-nums">
            {formatPhp(totalSpent)}
          </div>
          <div className="text-[10px] text-[#0F522B] font-semibold mt-0.5">
            Passenger completed trips
          </div>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="grid grid-cols-3 gap-1.5 p-1 rounded-2xl bg-[#EBF1ED]/90 border border-[#0F522B]/10">
        {([
          {id: 'all', label: 'All'},
          {id: 'completed', label: 'Completed'},
          {id: 'cancelled', label: 'Cancelled'},
        ] as const).map((tab) => {
          const active = filter === tab.id;
          return (
            <button
              key={tab.id}
              type="button"
              onClick={() => setFilter(tab.id)}
              className={`h-9 rounded-xl text-xs font-bold transition-colors ${
                active
                  ? 'bg-[#0F522B] text-white shadow-sm'
                  : 'text-[#55625A] hover:text-[#101713]'
              }`}
            >
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* Orders List */}
      {filteredOrders.length === 0 ? (
        <div className="p-5 rounded-2xl glass-surface text-center space-y-1.5">
          <div className="text-xs font-bold text-[#101713]">No orders in this view</div>
          <p className="text-[11px] text-[#55625A]">
            Completed and cancelled rides will appear here automatically.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {filteredOrders.map((order) => {
            const isCompleted = order.status === 'completed';
            const isRiderJob = order.roleContext === 'rider';
            return (
              <div
                key={order.id}
                className="p-3.5 rounded-2xl glass-surface space-y-2.5"
              >
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-2 min-w-0">
                    {order.vehicleType === 'motorcycle' ? (
                      <ByaheroMotorcycleVector className="w-9 h-6 shrink-0" lightSurface />
                    ) : (
                      <ByaheroTaxiVector className="w-9 h-6 shrink-0" lightSurface />
                    )}
                    <div className="min-w-0">
                      <div className="text-xs font-bold text-[#101713] truncate">
                        {order.vehicleType === 'motorcycle' ? 'BYAHERO Moto' : 'BYAHERO Cab'}
                        {isRiderJob ? ' · Rider Trip' : ''}
                      </div>
                      <div className="text-[10px] text-[#55625A] inline-flex items-center gap-1">
                        <Clock className="w-3 h-3" />
                        {formatTimestamp(order.createdAt)}
                      </div>
                    </div>
                  </div>

                  <div className="text-right shrink-0">
                    <div className="text-sm font-extrabold text-[#101713] tabular-nums">
                      {isRiderJob
                        ? `+${formatPhp(order.fare.riderTotalPayout)}`
                        : formatPhp(order.fare.finalFare)}
                    </div>
                    <span
                      className={`inline-flex items-center gap-1 text-[10px] font-bold ${
                        isCompleted ? 'text-[#0F522B]' : 'text-red-700'
                      }`}
                    >
                      {isCompleted ? (
                        <CheckCircle2 className="w-3 h-3" />
                      ) : (
                        <XCircle className="w-3 h-3" />
                      )}
                      {isCompleted ? 'Completed' : 'Cancelled'}
                    </span>
                  </div>
                </div>

                <div className="pt-2 border-t border-[#0F522B]/10 space-y-1.5 text-xs">
                  <div className="flex items-start gap-2">
                    <MapPin className="w-3.5 h-3.5 text-[#101713] mt-0.5 shrink-0" />
                    <span className="font-semibold text-[#101713] truncate">
                      {order.pickup.label}
                    </span>
                  </div>
                  <div className="flex items-start gap-2">
                    <Navigation className="w-3.5 h-3.5 text-[#0F522B] mt-0.5 shrink-0" />
                    <span className="font-semibold text-[#0F522B] truncate">
                      {order.destination.label}
                    </span>
                  </div>
                </div>

                <div className="flex items-center justify-between gap-2 pt-1 text-[11px] text-[#55625A]">
                  <span className="truncate tabular-nums">
                    {order.distanceKm.toFixed(1)} km · {order.durationMins} min
                    {order.riderName ? ` · ${order.riderName}` : ''}
                  </span>
                  {!isRiderJob && (
                    <button
                      type="button"
                      onClick={() => onRebookOrder(order)}
                      className="h-8 px-3 rounded-xl bg-[#EBF1ED] hover:bg-[#DCE7E0] text-[#0F522B] text-[11px] font-bold inline-flex items-center gap-1 whitespace-nowrap shrink-0 transition-colors"
                    >
                      <RotateCcw className="w-3 h-3" />
                      <span>Book Again</span>
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
