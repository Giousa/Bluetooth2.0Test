package com.zmm.bluetoothdemo;

import android.bluetooth.BluetoothDevice;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Description:
 * Author:zhangmengmeng
 * Date:2017/3/27
 * Time:下午1:05
 */

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

    private List<BluetoothDevice> mBluetoothDeviceList;
    private OnItemSelectedListener mItemSelectedListener;


    public MyAdapter(List<BluetoothDevice> bluetoothBeanList) {
        mBluetoothDeviceList = bluetoothBeanList;
    }

    public interface OnItemSelectedListener{
        void OnItemSelect(int position);
    }
    public void setOnItemSelectedListener(OnItemSelectedListener onItemSelectedListener){
        mItemSelectedListener = onItemSelectedListener;
    }

    /**
     * 创建view，绑定holder
     * @param parent
     * @param viewType
     * @return
     */
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view =  View.inflate(parent.getContext(), R.layout.my_item, null);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.setData(position);
    }

    @Override
    public int getItemCount() {
        return mBluetoothDeviceList.size();
    }

    public void add(BluetoothDevice bluetoothBean, int position) {
        mBluetoothDeviceList.add(position,bluetoothBean);
        notifyItemInserted(position);
        notifyDataSetChanged();
    }

    View.OnClickListener onClickListener =  new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            int position = (int) v.getTag();
            if(mItemSelectedListener != null){
                mItemSelectedListener.OnItemSelect(position);
            }
        }
    };

    class MyViewHolder extends RecyclerView.ViewHolder{

        private final TextView item_name,item_address;

        public MyViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(onClickListener);

            item_name = (TextView) itemView.findViewById(R.id.item_name);
            item_address = (TextView) itemView.findViewById(R.id.item_address);
        }

        /**
         * 根据position位置获取数据填充位置
         * @param position
         */
        public void setData(int position) {
            itemView.setTag(position);
            item_name.setText(mBluetoothDeviceList.get(position).getName());
            item_address.setText(mBluetoothDeviceList.get(position).getAddress());


        }
    }
}
