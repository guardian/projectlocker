import React from 'react';
import SortableTable from 'react-sortable-table';
import GeneralListComponent from './GeneralListComponent.jsx';

class StorageListComponent extends GeneralListComponent {
    constructor(props){
        super(props);
        this.endpoint = '/api/storage';
        this.columns = [
            {
                header: "Id",
                key: "id",
                defaultSorting: "desc",
                dataProps: { className: 'align-right'},
                headerProps: { className: 'dashboardheader'}
            },
            GeneralListComponent.standardColumn("Type","type"),
            GeneralListComponent.standardColumn("Root path","rootpath"),
            GeneralListComponent.standardColumn("Storage type","storageType"),
            GeneralListComponent.standardColumn("Username","user"),
            {
                header: "Password",
                key: "password",
                headerProps: { className: 'dashboardheader'},
                render: (passwd)=>{
                    if(!passwd) return <span style={{fontStyle: "italic"}}>n/a</span>;
                    let rtnstring="";
                    for(let n=0;n<passwd.length;n+=1){
                        rtnstring += "*";
                    }
                    return rtnstring;
                }
            },
            GeneralListComponent.standardColumn("Hostname (if applicable)","host"),
            GeneralListComponent.standardColumn("Port","port")
        ];
    }

    newElementCallback(event) {
        window.location = "/storage/new";
    }
}

export default StorageListComponent;