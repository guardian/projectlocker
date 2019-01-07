import React from 'react';
import axios from 'axios';
import SortableTable from 'react-sortable-table';
import {Link} from 'react-router-dom';
import moment from 'moment';

class GeneralListComponent extends React.Component {
    static ITEM_LIMIT=50;

    constructor(props){
        super(props);
        this.state = {
            data: [],
            hovered: false,
            filterTerms: {
                match: "W_ENDSWITH"
            },
            currentPage: 0,
            maximumItemsLoaded: false,
            plutoConfig: {},
            uid: "",
            isAdmin: false
        };

        this.pageSize = 20;

        this.gotDataCallback = this.gotDataCallback.bind(this);
        this.newElementCallback = this.newElementCallback.bind(this);
        this.filterDidUpdate = this.filterDidUpdate.bind(this);

        /* this must be supplied by a subclass */
        this.endpoint='/unknown';
        this.filterEndpoint = null;

        this.style = {
            backgroundColor: '#eee',
            border: '1px solid black',
            borderCollapse: 'collapse'
        };

        this.iconStyle = {
            color: '#aaa',
            paddingLeft: '5px',
            paddingRight: '5px'
        };

        this.canCreateNew=true;
        /* this must be supplied by a subclass */
        this.columns = [

        ];
    }

    componentDidMount() {
        this.loadDependencies().then(()=>{
            this.dependenciesDidLoad();
            this.reload();
        });
    }

    /**
     * override this in a subclass to update state once dependencies have loaded
     */
    dependenciesDidLoad(){

    }

    loadDependencies(){
        return new Promise((accept,reject)=>axios.get("/api/isLoggedIn")
            .then(response=>{
                if(response.data.status==="ok")
                    this.setState({isAdmin: response.data.isAdmin, uid: response.data.uid}, ()=>accept());
            })
            .catch(error=>{
                if(this.props.error.response && this.props.error.response.status===403)
                    this.setState({isAdmin: false}, ()=>accept());
                else {
                    console.error(error);
                    this.setState({isAdmin: false, error: error}, ()=>reject());
                }
            })
        );
    }

    /* this method supplies a column definition as a convenience for subclasses */
    static standardColumn(name, key) {
        return {
            header: name,
            key: key,
            headerProps: { className: 'dashboardheader'},
            render: (value)=><span style={{fontStyle: "italic"}}>n/a</span> ? value : (value && value.length>0)
        };
    }

    static boolColumn(name, key) {
        return {
            header: name,
            key: key,
            headerProps: { className: 'dashboardheader'},
            render: value=><span>{ String( value ) }</span>
        };
    }

    /* this method supplies a column definition for datetimes */
    static dateTimeColumn(name,key) {
        return {
            header: name,
            key: key,
            headerProps: {className: 'dashboardheader'},
            render: value=><span className="datetime">{moment(value).format("ddd Do MMM, HH:mm")}</span>
        }
    }

    breakdownPathComponents() {
        return this.props.location.pathname.split('/')
    }

    /* this method supplies the edit and delete icons. Can't be static as <Link> relies on the object context to access
     * history. */
    actionIcons() {
        const componentName = this.breakdownPathComponents()[1];
        return {
            header: "",
            key: "id",
            render: (id) => <span className="icons" style={{display: this.state.isAdmin ? "inherit" : "none"}}>
                    <Link to={"/" + componentName + "/" + id}><img className="smallicon" src="/assets/images/edit.png"/></Link>
                    <Link to={"/" + componentName + "/" + id + "/delete"}><img className="smallicon" src="/assets/images/delete.png"/></Link>
            </span>
        }
    }

    /* loads the next page of data */
    getNextPage(){
        const startAt = this.state.currentPage * this.pageSize;
        const length = this.pageSize;

        const axiosFuture = this.filterEndpoint ?
            axios.put(this.filterEndpoint + "?startAt=" + startAt + "&length=" + length, this.state.filterTerms) :
            axios.get(this.endpoint + "?startAt=" + startAt + "&length=" + length);

        axiosFuture.then(response=>{
            this.setState({
                currentPage: this.state.currentPage+1
            }, ()=>{
                this.gotDataCallback(response, ()=> {
                    if (response.data.result.length > 0)
                        if(this.pageSize*this.state.currentPage>=GeneralListComponent.ITEM_LIMIT)
                            this.setState({maximumItemsLoaded: true});
                        else
                            this.getNextPage()
                });
            })
        }).catch(error=>{
            console.error(error);
        });
    }

    /* reloads the data for the component based on the endpoint configured in the constructor */
    reload(){
        this.setState({
            currentPage: 0,
            data: []
        }, ()=> this.getNextPage());
    }

    /* called when we receive data; can be over-ridden by a subclass to do something more clever */
    gotDataCallback(response, cb){
        this.setState({
            data: this.state.data.concat(response.data.result)
        }, cb);
    }

    /* called when the New button is clicked; can be over-ridden by a subclass to do something more clever */
    newElementCallback(event){

    }

    /* called to insert a filtering component; should be over-ridden by a subclass if filtering is required */
    getFilterComponent(){
        return <span/>
    }

    /* this can be referenced from a filter component in a subclass and should be called to update the active filtering.
    this will cause a reload of data from the server
     */
    filterDidUpdate(newterms){
        this.setState({filterTerms: newterms},()=>this.reload());
    }

    itemLimitWarning(){
        if(this.state.maximumItemsLoaded)
            return <p className="warning-text"><i className="fa-info fa" style={{marginRight: "0.5em", color: "orange"}}/>Maximum of {GeneralListComponent.ITEM_LIMIT} items have been loaded. Use filters to narrow this down.</p>
        else
            return <p style={{margin: 0}}/>
    }

    render() {
        return (
            <div>
                <span className="list-title"><h2 className="list-title">{this.props.title}</h2></span>
                {this.getFilterComponent()}
                {this.itemLimitWarning()}

                <span className="banner-control">
                    <button id="newElementButton" onClick={this.newElementCallback}>New</button>
                </span>
                <SortableTable
                    data={ this.state.data}
                    columns={this.columns}
                    style={this.style}
                    iconStyle={this.iconStyle}
                    tableProps={ {className: "dashboardpanel"} }
                />

            </div>
        );
    }
}

export default GeneralListComponent;