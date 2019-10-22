import React from 'react';
import SortableTable from 'react-sortable-table';
import GeneralListComponent from './GeneralListComponent.jsx';
import StatusIndicator from "./EntryViews/StatusIndicator.jsx";

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
            {
                header: "Status",
                key: "status",
                dataProps: { className: 'align-right'},
                headerProps: {className: 'dashboardheader'},
                render: (status)=><StatusIndicator status={status}/>
            },
            GeneralListComponent.standardColumn("Type","storageType"),
            GeneralListComponent.standardColumn("Root path","rootpath"),
            GeneralListComponent.standardColumn("Client-facing path (if any)","clientpath"),
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
            GeneralListComponent.standardColumn("Port","port"),
            GeneralListComponent.boolColumn("Versioning Enabled", "supportsVersions"),
            this.actionIcons()
        ];
    }

    newElementCallback(event) {
        this.props.history.push("/storage/new");
    }
}

export default StorageListComponent;